package funds.repository;

import funds.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AnalyticsRepository {

    List<ComplianceRules> findByIsActiveTrueOrderByPriorityAsc();

    List<RawFundSource> findRawFundsForQuarter(String tag, LocalDate minDate, LocalDate maxDate);

    List<RawBenchmarkSource> findAllBenchmarksByTags(List<String> includedTags, List<String> excludedTags);

    List<BenchmarkCandidate> fetchBenchmarkProviderRules();

    void saveAllBenchmarks(List<? extends BenchmarkMaster> benchmarks);

    Map<String, List<BenchmarkMaster>> mapfindBenchmarksGroupedByAccession();

    Map<String, String> findFundNamesByTag(String tag);

    void saveAllFunds(List<? extends FundMaster> items);

    void upsertFundBenchmarkAssociations(List<FundBenchmarkAssociation> associations);
}
