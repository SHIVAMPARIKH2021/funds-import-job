package funds.repository.impl;

import ch.qos.logback.core.util.StringUtil;
import funds.model.*;
import funds.repository.AnalyticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;;

    @Autowired
    private final String findActiveComplianceRules;

    @Autowired
    private final String getRawFundData;

    @Autowired
    private final String findBenchmarkByName;

    @Autowired
    private final String findAllBenchmarksByTags;

    @Autowired
    private final String fetchBenchmarkProviderRules;

    @Autowired
    private final String findFundBySeriesIdSql;

    public AnalyticsRepositoryImpl(JdbcTemplate jdbcTemplate,
                                   NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                   String findActiveComplianceRules,
                                   String getRawFundData,
                                   String findBenchmarkByName,
                                   String findAllBenchmarksByTags,
                                   String fetchBenchmarkProviderRules,
                                   String findFundBySeriesIdSql) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.findActiveComplianceRules = findActiveComplianceRules;
        this.getRawFundData = getRawFundData;
        this.findBenchmarkByName = findBenchmarkByName;
        this.findAllBenchmarksByTags = findAllBenchmarksByTags;
        this.fetchBenchmarkProviderRules = fetchBenchmarkProviderRules;
        this.findFundBySeriesIdSql = findFundBySeriesIdSql;
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
        return jdbcTemplate.query(findActiveComplianceRules, new BeanPropertyRowMapper<>());
    }

    @Override
    public Optional<FundMaster> findBySeriesId(String seriesId) {
        if (seriesId == null || seriesId.isBlank()) {
            return Optional.empty();
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("seriesId", seriesId.trim());

        List<FundMaster> results = namedParameterJdbcTemplate.query(
                findFundBySeriesIdSql,
                params,
                new BeanPropertyRowMapper<>(FundMaster.class)
        );

        return results.stream().findFirst();
    }

    @Override
    public List<RawFundSource> findRawFundsByVersionAndTag(String version, String tag) {
        MapSqlParameterSource MapSqlParameterSource = new MapSqlParameterSource()
                .addValue("version", version)
                .addValue("tag", tag);
        return namedParameterJdbcTemplate.query(getRawFundData, MapSqlParameterSource, new BeanPropertyRowMapper<>(RawFundSource.class));
    }

    @Override
    public List<String> findAllBenchmarksByTags(List<String> includedTags, List<String> excludedTags) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("benchmarkTags", includedTags)
                .addValue("excludedBenchmarks", excludedTags);
        return namedParameterJdbcTemplate.query(findAllBenchmarksByTags, params, new BeanPropertyRowMapper<>(String.class));
    }

    @Override
    public List<BenchmarkCandidate> findAllBenchmarks() {
        return jdbcTemplate.query(fetchBenchmarkProviderRules, new BeanPropertyRowMapper<>(BenchmarkCandidate.class));
    }
}
