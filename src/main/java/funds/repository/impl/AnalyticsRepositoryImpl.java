package funds.repository.impl;

import ch.qos.logback.core.util.StringUtil;
import funds.model.*;
import funds.repository.AnalyticsRepository;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;

@Repository
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final String findActiveComplianceRules;
    private final String getRawFundData;
    private final String findBenchmarkByName;
    private final String findAllBenchmarksByTags;
    private final String fetchBenchmarkProviderRules;
    private final String upsertBenchmarkMasterQuery;
    private final String findAllBenchmark;
    private final String findAccessionNumberFromBenchmark;
    private final String findBenchmarksGroupedByAccession;
    private final String upsertFundMasterQuery;
    private final String findFundNamesByTagSql;


    public AnalyticsRepositoryImpl(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Qualifier("findActiveComplianceRules") String findActiveComplianceRules,
            @Qualifier("getRawFundData") String getRawFundData,
            @Qualifier("findBenchmarkByAccessionNumbers") String findBenchmarkByName,
            @Qualifier("findAllBenchmarksByTags") String findAllBenchmarksByTags,
            @Qualifier("fetchBenchmarkProviderRules") String fetchBenchmarkProviderRules,
            @Qualifier("upsertBenchmarkMasterQuery") String upsertBenchmarkMasterQuery,
            @Qualifier("upsertBenchmarkMaster") String findAllBenchmark,
            @Qualifier("findAccessionNumberFromBenchmark") String findAccessionNumberFromBenchmark,
            @Qualifier("findBenchmarksGroupedByAccession") String findBenchmarksGroupedByAccession,
            @Qualifier("upsertFundMasterQuery") String upsertFundMasterQuery,
            @Qualifier("findFundNamesByTagSql") String findFundNamesByTagSql) {

        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.findActiveComplianceRules = findActiveComplianceRules;
        this.getRawFundData = getRawFundData;
        this.findBenchmarkByName = findBenchmarkByName;
        this.findAllBenchmarksByTags = findAllBenchmarksByTags;
        this.fetchBenchmarkProviderRules = fetchBenchmarkProviderRules;
        this.upsertBenchmarkMasterQuery = upsertBenchmarkMasterQuery;
        this.findAllBenchmark = findAllBenchmark;
        this.findAccessionNumberFromBenchmark = findAccessionNumberFromBenchmark;
        this.findBenchmarksGroupedByAccession = findBenchmarksGroupedByAccession;
        this.upsertFundMasterQuery = upsertFundMasterQuery;
        this.findFundNamesByTagSql = findFundNamesByTagSql;
    }

    @Override
    public Optional<BenchmarkMaster> findBenchMarkByBenchmarkName(String benchmarkId) {
        if (StringUtil.isNullOrEmpty(benchmarkId)) {
            return Optional.empty();
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("benchmarkId", benchmarkId.trim());

        // query(...) avoiding EmptyResultDataAccessException
        List<BenchmarkMaster> results = namedParameterJdbcTemplate.query(
                findBenchmarkByName,
                params,
                new BeanPropertyRowMapper<>(BenchmarkMaster.class)
        );

        return results.stream().findFirst();
    }

    @Override
    public List<ComplianceRules> findByIsActiveTrueOrderByPriorityAsc() {
        return jdbcTemplate.query(findActiveComplianceRules, new BeanPropertyRowMapper<>(ComplianceRules.class));
    }

    @Override
    public List<RawFundSource> findRawFundsForQuarter(String tag, LocalDate minDate, LocalDate maxDate) {
        MapSqlParameterSource MapSqlParameterSource = new MapSqlParameterSource()
                .addValue("tag", tag)
                .addValue("minDate", minDate)
                .addValue("maxDate", maxDate);
        return namedParameterJdbcTemplate.query(getRawFundData, MapSqlParameterSource, new BeanPropertyRowMapper<>(RawFundSource.class));
    }

    @Override
    public List<RawBenchmarkSource> findAllBenchmarksByTags(List<String> includedTags, List<String> excludedTags) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("benchmarkTags", includedTags)
                .addValue("excludedBenchmarks", excludedTags);
        return namedParameterJdbcTemplate.query(findAllBenchmarksByTags, params, new BeanPropertyRowMapper<>(RawBenchmarkSource.class));
    }

    @Override
    public List<BenchmarkCandidate> fetchBenchmarkProviderRules() {
        return jdbcTemplate.query(fetchBenchmarkProviderRules, new BeanPropertyRowMapper<>(BenchmarkCandidate.class));
    }

    @Override
    public void saveBenchmark(String benchmarkName, String benchmarkProvider, String benchmarkType, String createdBy) {
        jdbcTemplate.update(upsertBenchmarkMasterQuery, benchmarkName, benchmarkProvider, benchmarkType, createdBy);
    }

    @Override
    public void saveAllBenchmarks(List<? extends BenchmarkMaster> benchmarks) {
        if (benchmarks == null || benchmarks.isEmpty()) {
            return;
        }
        namedParameterJdbcTemplate.batchUpdate(
                upsertBenchmarkMasterQuery,
                SqlParameterSourceUtils.createBatch(benchmarks)
        );
    }

    public void saveAllFunds(List<? extends FundMaster> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        namedParameterJdbcTemplate.batchUpdate(
                upsertFundMasterQuery,
                SqlParameterSourceUtils.createBatch(items)
        );
    }

    public List<BenchmarkMaster> findBenchmarkMasterByAccessionNumbers(List<String> accessionNumbers) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("accessionNumbers", accessionNumbers);
        return jdbcTemplate.query(findAllBenchmark, new BeanPropertyRowMapper<>(BenchmarkMaster.class));
    }

    @Override
    public List<String> findAccessionNumberFromBenchmark() {
        return namedParameterJdbcTemplate.query(
                findAccessionNumberFromBenchmark,
                new BeanPropertyRowMapper<>(String.class)
        );
    }

    @Override
    public Map<String, List<BenchmarkMaster>> mapfindBenchmarksGroupedByAccession() {
        Map<String, List<BenchmarkMaster>> map = new HashMap<>();
        namedParameterJdbcTemplate.query(findBenchmarksGroupedByAccession, rs -> {
            String accession = rs.getString("accessionNumber").strip();

            BenchmarkMaster bm = new BenchmarkMaster();
            bm.setAccessionNumber(accession);
            bm.setBenchmarkId(rs.getObject("benchmarkId", UUID.class)); // Native UUID
            bm.setBenchmarkName(rs.getString("benchmarkName"));
            bm.setBenchmarkType(rs.getString("benchmarkType"));

            map.computeIfAbsent(accession, k -> new ArrayList<>()).add(bm);
        });
        return map;
    }

    @Override
    public Map<String, String> findFundNamesByTag(String tag) {
        Map<String, String> fundNamesMap = new HashMap<>();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tag", tag);

        namedParameterJdbcTemplate.query(findFundNamesByTagSql, params, rs -> {
            String seriesId = rs.getString("seriesId");
            String fundName = rs.getString("fundName");
            fundNamesMap.put(seriesId, fundName);
        });

        return fundNamesMap;
    }
}
