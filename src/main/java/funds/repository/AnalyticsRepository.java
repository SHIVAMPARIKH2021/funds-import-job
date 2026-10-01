package funds.repository;

import funds.model.*;

import java.util.List;
import java.util.Optional;

public interface AnalyticsRepository {
    Optional<BenchmarkMaster> findBenchMarkByBenchmarkName(String benchmarkName);

    List<ComplianceRules> findByIsActiveTrueOrderByPriorityAsc();

    Optional<FundMaster> findBySeriesId(String seriesId);

    List<RawFundSource> findRawFundsByVersionAndTag(String version, String tag);

    List<String> findAllBenchmarksByTags(List<String> includedTags, List<String> excludedTags);


    List<BenchmarkCandidate> findAllBenchmarks();
}
