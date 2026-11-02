package funds.repository;

import funds.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AnalyticsRepository {

    List<ComplianceRules> findByIsActiveTrueOrderByPriorityAsc();

    List<RawFundSource> findRawFundsForQuarter(List<String> strategyTags, List<String> objectiveTags, LocalDate minDate, LocalDate maxDate);

    List<RawBenchmarkSource> findAllBenchmarksByTags(List<String> includedTags, List<String> excludedTags);

    List<BenchmarkCandidate> fetchBenchmarkProviderRules();

    void saveAllBenchmarks(List<? extends BenchmarkMaster> benchmarks);

    Map<String, List<BenchmarkMaster>> mapfindBenchmarksGroupedByAccessionAndSeries();

    Map<String, String> findFundNamesByTag(String tag);

    void saveAllFunds(List<? extends FundMaster> items);

    void upsertFundBenchmarkAssociations(List<FundBenchmarkAssociation> associations);

    Integer getOrCreateReportingQuarter(int year, int quarter, String executor);

    void upsertQuarterFundAssociations(List<QuarterFundAssociation> associations);

    List<RawBenchmarkSource> findUnresolvedBenchmarksFromTextFacts(List<String> benchmarkTags, String accessionNumber);

    Set<String> findSeriesWithActivePrimaryBenchmark();
}
