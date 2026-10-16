package funds.repository;

import funds.model.BenchmarkMaster;
import funds.model.BenchmarkCandidate;
import funds.model.ComplianceRules;
import funds.model.RawFundSource;
import funds.model.RawBenchmarkSource;
import funds.model.FundBenchmarkAssociation;
import funds.model.FundMaster;
import funds.repository.impl.AnalyticsRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.mockito.Mock;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class AnalyticsRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private AnalyticsRepositoryImpl analyticsRepositoryImpl;

    private final String findActiveComplianceRules = "SELECT * FROM compliance_rules WHERE is_active = TRUE ORDER BY priority ASC";
    private final String getRawFundData = "SELECT * FROM raw_fund_data";
    private final String findAllBenchmarksByTags = "SELECT * FROM benchmarks";
    private final String fetchBenchmarkProviderRules = "SELECT * FROM benchmark_provider_rules";
    private final String upsertBenchmarkMasterQuery = "INSERT INTO benchmark_master";
    private final String upsertFundMasterQuery = "INSERT INTO fund_master";
    private final String findFundNamesByTagSql = "SELECT * FROM fund_names_by_tag";
    private final String upsertFundBenchmarkAssociationQuery = "INSERT INTO fund_benchmark_association";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        String findBenchmarksGroupedByAccession = "SELECT * FROM benchmark_master GROUP BY accession_number";
        String upsertReportingQuarterSql = "INSERT INTO reporting_quarter";
        String upsertQuarterFundAssociationSql = "INSERT INTO quarter_fund_association";
        analyticsRepositoryImpl = new AnalyticsRepositoryImpl(
                jdbcTemplate,
                namedParameterJdbcTemplate,
                findActiveComplianceRules,
                getRawFundData,
                "findBenchmarkByAccessionNumbers",
                findAllBenchmarksByTags,
                fetchBenchmarkProviderRules,
                upsertBenchmarkMasterQuery,
                "findAccessionNumberFromBenchmark",
                findBenchmarksGroupedByAccession,
                upsertFundMasterQuery,
                findFundNamesByTagSql,
                upsertFundBenchmarkAssociationQuery,
                upsertReportingQuarterSql,
                upsertQuarterFundAssociationSql);
    }

    @Test
    void findByIsActiveTrueOrderByPriorityAsc() {
        List<ComplianceRules> expected = Collections.singletonList(new ComplianceRules(true, true, "pattern", "rule", 1, 1, "strategy"));
        doReturn(expected).when(jdbcTemplate).query(eq(findActiveComplianceRules), any(BeanPropertyRowMapper.class));

        List<ComplianceRules> actual = analyticsRepositoryImpl.findByIsActiveTrueOrderByPriorityAsc();

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("pattern", actual.getFirst().getPattern());
        verify(jdbcTemplate).query(eq(findActiveComplianceRules), any(BeanPropertyRowMapper.class));
    }

    @Test
    void findRawFundsForQuarter() {
        List<RawFundSource> expected = Collections.singletonList(new RawFundSource());
        doReturn(expected).when(namedParameterJdbcTemplate).query(eq(getRawFundData), any(SqlParameterSource.class), any(BeanPropertyRowMapper.class));

        List<RawFundSource> actual = analyticsRepositoryImpl.findRawFundsForQuarter(
                Collections.singletonList("tag"), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31));

        assertNotNull(actual);
        assertEquals(1, actual.size());
        verify(namedParameterJdbcTemplate).query(eq(getRawFundData), any(SqlParameterSource.class), any(BeanPropertyRowMapper.class));
    }

    @Test
    void findAllBenchmarksByTags() {
        List<RawBenchmarkSource> expected = Collections.singletonList(new RawBenchmarkSource());
        doReturn(expected).when(namedParameterJdbcTemplate).query(eq(findAllBenchmarksByTags), any(SqlParameterSource.class), any(BeanPropertyRowMapper.class));

        List<RawBenchmarkSource> actual = analyticsRepositoryImpl.findAllBenchmarksByTags(
                Collections.emptyList(), Collections.emptyList());

        assertNotNull(actual);
        assertEquals(1, actual.size());
        verify(namedParameterJdbcTemplate).query(eq(findAllBenchmarksByTags), any(SqlParameterSource.class), any(BeanPropertyRowMapper.class));
    }

    @Test
    void fetchBenchmarkProviderRules() {
        BenchmarkCandidate candidate = new BenchmarkCandidate();
        List<BenchmarkCandidate> expected = Collections.singletonList(candidate);
        doReturn(expected).when(jdbcTemplate).query(eq(fetchBenchmarkProviderRules), any(BeanPropertyRowMapper.class));

        List<BenchmarkCandidate> actual = analyticsRepositoryImpl.fetchBenchmarkProviderRules();

        assertNotNull(actual);
        assertEquals(1, actual.size());
        verify(jdbcTemplate).query(eq(fetchBenchmarkProviderRules), any(BeanPropertyRowMapper.class));
    }

    @Test
    void saveAllBenchmarks() {
        List<BenchmarkMaster> benchmarks = Collections.singletonList(new BenchmarkMaster());
        analyticsRepositoryImpl.saveAllBenchmarks(benchmarks);
        verify(namedParameterJdbcTemplate).batchUpdate(eq(upsertBenchmarkMasterQuery), any(SqlParameterSource[].class));
    }

    @Test
    void saveAllFunds() {
        List<FundMaster> funds = Collections.singletonList(new FundMaster());
        analyticsRepositoryImpl.saveAllFunds(funds);
        verify(namedParameterJdbcTemplate).batchUpdate(eq(upsertFundMasterQuery), any(SqlParameterSource[].class));
    }

    @Test
    void mapfindbenchmarksgroupedbyaccession() {
        Map<String, List<BenchmarkMaster>> actual = analyticsRepositoryImpl.mapfindBenchmarksGroupedByAccession();
        assertNotNull(actual);
    }

    @Test
    void findFundNamesByTag() {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("tag", "tag");
        doReturn(Collections.emptyList()).when(namedParameterJdbcTemplate).query(eq(findFundNamesByTagSql), eq(params), org.mockito.ArgumentMatchers.any(ResultSetExtractor.class));

        Map<String, String> actual = analyticsRepositoryImpl.findFundNamesByTag("tag");

        assertNotNull(actual);
        assertEquals(0, actual.size());
    }

    @Test
    void upsertFundBenchmarkAssociations() {
        List<FundBenchmarkAssociation> associations = Collections.singletonList(new FundBenchmarkAssociation("series1", 1, "BM1", true, "ACC1"));
        analyticsRepositoryImpl.upsertFundBenchmarkAssociations(associations);
        verify(namedParameterJdbcTemplate).batchUpdate(eq(upsertFundBenchmarkAssociationQuery), any(SqlParameterSource[].class));
    }
}
