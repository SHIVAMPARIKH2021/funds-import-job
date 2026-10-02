package funds.processor;

import ch.qos.logback.core.util.StringUtil;
import funds.constants.Tags;
import funds.model.FundMaster;
import funds.model.BenchmarkMaster;
import funds.model.ComplianceRules;
import funds.model.RawFundSource;
import funds.repository.AnalyticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
@StepScope
public class FundsItemProcessor implements ItemProcessor<RawFundSource, List<FundMaster>> {

    @Value("#{jobParameters['executor'] ?: 'SYSTEM'}")
    private String executor;

    @Value("#{jobParameters['dryrun'] ?: 'false'}")
    private String dryRun;

    private final AnalyticsRepository analyticsRepository;
    private final Map<Integer, Pattern> complianceRuleMap = new HashMap<>();
    private List<ComplianceRules> complianceRules = Collections.emptyList();

    // Grouped 1:N cache loaded once per step: accession_number -> List<BenchmarkMaster>
    private Map<String, List<BenchmarkMaster>> benchmarksByAccessionMap = Collections.emptyMap();

    private Map<String, String> fundNamesByTagMap = Collections.emptyMap();

    public FundsItemProcessor(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @BeforeStep
    public void setup(StepExecution stepExecution) {
        // 1. Pre-compile active compliance regex rules
        this.complianceRules = analyticsRepository.findByIsActiveTrueOrderByPriorityAsc();
        this.complianceRuleMap.clear();

        for (ComplianceRules rule : this.complianceRules) {
            if (Boolean.TRUE.equals(rule.getIsRegex()) && rule.getPattern() != null) {
                this.complianceRuleMap.put(
                        rule.getRuleId(),
                        Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                );
            }
        }

        // 2. Preload grouped benchmarks by accession number (O(1) memory lookup)
        this.benchmarksByAccessionMap = analyticsRepository.mapfindBenchmarksGroupedByAccession();

        this.fundNamesByTagMap.clear();
        this.fundNamesByTagMap = analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());

        log.info("Initialized {} compliance rules and {} distinct accession benchmarks for step '{}'",
                this.complianceRules.size(), this.benchmarksByAccessionMap.size(), stepExecution.getStepName());
    }

    @Override
    public List<FundMaster> process(RawFundSource item) throws Exception {
        if (item == null || StringUtil.isNullOrEmpty(item.getSeriesId())) {
            return null; // Skip invalid records
        }

        if (Boolean.parseBoolean(this.dryRun)) {
            return null;
        }

        String classifiedStrategy = evaluateStrategy(item.getStrategyNarrative());
        String accessionNumber = item.getAccessionNumber() != null ? item.getAccessionNumber().strip() : null;

        List<BenchmarkMaster> matchedBenchmarks = accessionNumber != null
                ? this.benchmarksByAccessionMap.getOrDefault(accessionNumber, Collections.emptyList())
                : Collections.emptyList();

        List<FundMaster> fundEntries = new ArrayList<>();

        if (!matchedBenchmarks.isEmpty()) {
            // Fan-out: Create one fund_master record per associated benchmark
            for (BenchmarkMaster bm : matchedBenchmarks) {
                FundMaster fund = createBaseFundEntity(item, classifiedStrategy);
                fund.setBenchmarkId(bm.getBenchmarkId()); // java.util.UUID
                fund.setBenchmarkName(bm.getBenchmarkName());
                fundEntries.add(fund);
            }
        } else {
            // No comparative benchmark declared for this accession filing: emit single entry with null benchmark
            FundMaster fund = createBaseFundEntity(item, classifiedStrategy);
            fund.setBenchmarkId(null);
            fund.setBenchmarkName(null);
            fundEntries.add(fund);
        }

        return fundEntries;
    }

    private FundMaster createBaseFundEntity(RawFundSource item, String classifiedStrategy) {
        FundMaster fund = new FundMaster();
        fund.setFundFamily(item.getFundFamily());
        // Use the fund name from the tag mapping if available; otherwise, fallback to the fund family
        fund.setFundName(this.fundNamesByTagMap.getOrDefault(item.getSeriesId().strip(), item.getFundFamily()));
        fund.setSeriesId(item.getSeriesId().strip());
        fund.setCik(item.getCik());
        fund.setAccessionNumber(item.getAccessionNumber());
        fund.setPrimaryTicker(item.getTicker());
        fund.setStrategyNarrative(item.getStrategyNarrative());
        fund.setStrategyType(classifiedStrategy);
        fund.setCreatedBy(this.executor);
        fund.setUpdatedBy(this.executor);
        return fund;
    }

    private String evaluateStrategy(String narrative) {
        String UNKNOWN_TYPE = "UNKNOWN";
        if (StringUtil.isNullOrEmpty(narrative)) {
            return UNKNOWN_TYPE;
        }

        for (ComplianceRules rule : complianceRules) {
            boolean matches;
            if (Boolean.TRUE.equals(rule.getIsRegex())) {
                Pattern pattern = complianceRuleMap.get(rule.getRuleId());
                matches = pattern != null && pattern.matcher(narrative).find();
            } else {
                matches = narrative.toLowerCase().contains(rule.getPattern().toLowerCase());
            }

            if (matches) {
                return rule.getTargetStrategy();
            }
        }

        return UNKNOWN_TYPE;
    }
}