package funds.repository.impl;

import funds.model.FundMaster;
import funds.model.FundBenchmarkAssociation;
import funds.model.BenchmarkMaster;
import funds.model.BenchmarkCandidate;
import funds.model.ComplianceRules;
import funds.model.RawFundSource;
import funds.model.RawBenchmarkSource;
import funds.repository.AnalyticsRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;

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
    private final String findAccessionNumberFromBenchmark;
    private final String findBenchmarksGroupedByAccession;
    private final String upsertFundMasterQuery;
    private final String findFundNamesByTagSql;
    private final String upsertFundBenchmarkAssociationQuery;


    public AnalyticsRepositoryImpl(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Qualifier("findActiveComplianceRules") String findActiveComplianceRules,
            @Qualifier("getRawFundData") String getRawFundData,
            @Qualifier("findBenchmarkByAccessionNumbers") String findBenchmarkByName,
            @Qualifier("findAllBenchmarksByTags") String findAllBenchmarksByTags,
            @Qualifier("fetchBenchmarkProviderRules") String fetchBenchmarkProviderRules,
            @Qualifier("upsertBenchmarkMasterQuery") String upsertBenchmarkMasterQuery,
            @Qualifier("findAccessionNumberFromBenchmark") String findAccessionNumberFromBenchmark,
            @Qualifier("findBenchmarksGroupedByAccession") String findBenchmarksGroupedByAccession,
            @Qualifier("upsertFundMasterQuery") String upsertFundMasterQuery,
            @Qualifier("findFundNamesByTagSql") String findFundNamesByTagSql,
            @Qualifier("upsertFundBenchmarkAssociationQuery") String upsertFundBenchmarkAssociationQuery) {

        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.findActiveComplianceRules = findActiveComplianceRules;
        this.getRawFundData = getRawFundData;
        this.findBenchmarkByName = findBenchmarkByName;
        this.findAllBenchmarksByTags = findAllBenchmarksByTags;
        this.fetchBenchmarkProviderRules = fetchBenchmarkProviderRules;
        this.upsertBenchmarkMasterQuery = upsertBenchmarkMasterQuery;
        this.findAccessionNumberFromBenchmark = findAccessionNumberFromBenchmark;
        this.findBenchmarksGroupedByAccession = findBenchmarksGroupedByAccession;
        this.upsertFundMasterQuery = upsertFundMasterQuery;
        this.findFundNamesByTagSql = findFundNamesByTagSql;
        this.upsertFundBenchmarkAssociationQuery = upsertFundBenchmarkAssociationQuery;
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

    @Override
    public Map<String, List<BenchmarkMaster>> mapfindBenchmarksGroupedByAccession() {
        Map<String, List<BenchmarkMaster>> map = new HashMap<>();
        namedParameterJdbcTemplate.query(findBenchmarksGroupedByAccession, rs -> {
            String accession = rs.getString("accessionNumber").trim();

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

    @Override
    public void upsertFundBenchmarkAssociations(List<FundBenchmarkAssociation> associations) {
        if (associations == null || associations.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(associations.toArray());
        namedParameterJdbcTemplate.batchUpdate(this.upsertFundBenchmarkAssociationQuery, batch);
    }
}
