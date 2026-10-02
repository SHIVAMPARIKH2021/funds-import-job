package funds.repository;

import funds.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AnalyticsRepository {
    Optional<BenchmarkMaster> findBenchMarkByBenchmarkName(String benchmarkName);

    List<ComplianceRules> findByIsActiveTrueOrderByPriorityAsc();

    List<BenchmarkMaster> findBenchmarkMasterByAccessionNumbers(List<String> accessionNumbers);

    List<RawFundSource> findRawFundsForQuarter(String tag, LocalDate minDate, LocalDate maxDate);

    List<RawBenchmarkSource> findAllBenchmarksByTags(List<String> includedTags, List<String> excludedTags);

    List<BenchmarkCandidate> fetchBenchmarkProviderRules();

    void saveBenchmark(String benchmarkName, String benchmarkProvider, String benchmarkType, String createdBy);

    void saveAllBenchmarks(List<? extends BenchmarkMaster> benchmarks);

    List<String> findAccessionNumberFromBenchmark();

    Map<String, List<BenchmarkMaster>> mapfindBenchmarksGroupedByAccession();

    Map<String, String> findFundNamesByTag(String tag);
}
