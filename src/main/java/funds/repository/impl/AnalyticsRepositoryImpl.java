package funds.repository.impl;

import funds.model.RawBenchmarkSource;
import funds.model.BenchmarkCandidate;
import funds.model.BenchmarkMaster;
import funds.model.FundMaster;
import funds.model.RawFundSource;
import funds.model.ComplianceRules;
import funds.model.FundBenchmarkAssociation;
import funds.model.QuarterFundAssociation;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;

@Repository
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final String findActiveComplianceRules;
    private final String getRawFundData;
    private final String findAllBenchmarksByTags;
    private final String fetchBenchmarkProviderRules;
    private final String upsertBenchmarkMasterQuery;
    private final String findBenchmarksGroupedByAccession;
    private final String upsertFundMasterQuery;
    private final String findFundNamesByTagSql;
    private final String upsertFundBenchmarkAssociationQuery;
    private final String upsertReportingQuarterSql;
    private final String upsertQuarterFundAssociationSql;
    private final String findTextBenchmarkDisclosuresSql;
    private final String stageFilingBenchmarkSql;


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
            @Qualifier("upsertFundBenchmarkAssociationQuery") String upsertFundBenchmarkAssociationQuery,
            @Qualifier("upsertReportingQuarterSql") String upsertReportingQuarterSql,
            @Qualifier("upsertQuarterFundAssociationSql") String upsertQuarterFundAssociationSql,
            @Qualifier("findTextBenchmarkDisclosuresSql") String findTextBenchmarkDisclosuresSql,
            @Qualifier("stageFilingBenchmarkSql") String stageFilingBenchmarkSql) {

        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.findActiveComplianceRules = findActiveComplianceRules;
        this.getRawFundData = getRawFundData;
        this.findAllBenchmarksByTags = findAllBenchmarksByTags;
        this.fetchBenchmarkProviderRules = fetchBenchmarkProviderRules;
        this.upsertBenchmarkMasterQuery = upsertBenchmarkMasterQuery;
        this.findBenchmarksGroupedByAccession = findBenchmarksGroupedByAccession;
        this.upsertFundMasterQuery = upsertFundMasterQuery;
        this.findFundNamesByTagSql = findFundNamesByTagSql;
        this.upsertFundBenchmarkAssociationQuery = upsertFundBenchmarkAssociationQuery;
        this.upsertReportingQuarterSql = upsertReportingQuarterSql;
        this.upsertQuarterFundAssociationSql = upsertQuarterFundAssociationSql;
        this.findTextBenchmarkDisclosuresSql = findTextBenchmarkDisclosuresSql;
        this.stageFilingBenchmarkSql = stageFilingBenchmarkSql;
    }

    @Override
    public List<ComplianceRules> findByIsActiveTrueOrderByPriorityAsc() {
        return jdbcTemplate.query(findActiveComplianceRules, new BeanPropertyRowMapper<>(ComplianceRules.class));
    }

    @Override
    public List<RawFundSource> findRawFundsForQuarter(List<String> tags, LocalDate minDate, LocalDate maxDate) {
        MapSqlParameterSource MapSqlParameterSource = new MapSqlParameterSource()
                .addValue("tags", tags)
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
        SqlParameterSource[] params = SqlParameterSourceUtils.createBatch(benchmarks);

        namedParameterJdbcTemplate.batchUpdate(
                upsertBenchmarkMasterQuery,
                params
        );

        namedParameterJdbcTemplate.batchUpdate(stageFilingBenchmarkSql, params);
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
            bm.setBenchmarkId(rs.getObject("benchmarkId", Integer.class));
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

    @Override
    public Integer getOrCreateReportingQuarter(int year, int quarter, String executor) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("year", year)
                .addValue("quarter", quarter)
                .addValue("createdBy", executor);

        // upsertReportingQuarterSql returns quarter_id directly via RETURNING clause
        return namedParameterJdbcTemplate.queryForObject(
                upsertReportingQuarterSql,
                params,
                Integer.class
        );
    }

    @Override
    public void upsertQuarterFundAssociations(List<QuarterFundAssociation> associations) {
        SqlParameterSource[] batchParams = associations.stream()
                .map(assoc -> new MapSqlParameterSource()
                        .addValue("quarterId", assoc.getQuarterId())
                        .addValue("seriesId", assoc.getSeriesId())
                        .addValue("accessionNumber", assoc.getAccessionNumber())
                        .addValue("createdBy", assoc.getCreatedBy()))
                .toArray(SqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(upsertQuarterFundAssociationSql, batchParams);
    }

    @Override
    public List<RawBenchmarkSource> findUnresolvedBenchmarksFromTextFacts(List<String> includedTags, String accessionNumber) {
        // Query 2: Query sec_financials schema for text disclosures
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("benchmarkTags", includedTags)
                .addValue("accessionNumber", accessionNumber);

        List<RawBenchmarkSource> disclosures = namedParameterJdbcTemplate.query(
                findTextBenchmarkDisclosuresSql,
                params,
                new BeanPropertyRowMapper<>(RawBenchmarkSource.class)
        );

        // Filter: Keep disclosures that match unresolved funds
        return disclosures.stream()
                .filter(d -> d.getSeriesId() != null)
                .toList();
    }
}
