package funds.processor;

import ch.qos.logback.core.util.StringUtil;
import funds.constants.Tags;
import funds.model.FundMaster;
import funds.model.BenchmarkMaster;
import funds.model.FundBenchmarkAssociation;
import funds.model.RawFundSource;
import funds.model.FundImportPayload;
import funds.model.ComplianceRules;
import funds.repository.AnalyticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@StepScope
public class FundsItemProcessor implements ItemProcessor<RawFundSource, FundImportPayload> {

    private static final String ANNUAL_CADENCE = "ANNUAL";
    private static final String DEFAULT_BENCHMARK_NAME = "Not Disclosed / New Inception";

    private final String executor;
    private final String dryRun;
    private final int defaultBenchmarkId;
    private final AnalyticsRepository analyticsRepository;
    private final Map<Integer, Pattern> complianceRuleMap = new HashMap<>();
    private List<ComplianceRules> complianceRules = Collections.emptyList();

    // Grouped 1:N cache loaded once per step: accession_number -> List<BenchmarkMaster>
    private Map<String, List<BenchmarkMaster>> benchmarksByAccessionMap = Collections.emptyMap();

    private Map<String, String> fundNamesByTagMap = Collections.emptyMap();

    public FundsItemProcessor(AnalyticsRepository analyticsRepository,
                              @Value("#{jobParameters['executor']}") String executor,
                              @Value("#{jobParameters['dryrun']}") String dryRun,
                              @Value("${benchmark.default.id:0}") int defaultBenchmarkId
    ) {
        this.analyticsRepository = analyticsRepository;
        this.executor = executor;
        this.dryRun = dryRun;
        this.defaultBenchmarkId = defaultBenchmarkId;
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
        this.benchmarksByAccessionMap.clear();
        this.benchmarksByAccessionMap = analyticsRepository.mapfindBenchmarksGroupedByAccession();

        this.fundNamesByTagMap.clear();
        this.fundNamesByTagMap = analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());

        log.info("Initialized {} compliance rules and {} distinct accession benchmarks for step '{}'",
                this.complianceRules.size(), this.benchmarksByAccessionMap.size(), stepExecution.getStepName());
    }

    @Override
    public FundImportPayload process(RawFundSource item) {
        if (item == null || StringUtil.isNullOrEmpty(item.getSeriesId())) {
            return null; // Skip invalid records
        }

        if (Boolean.parseBoolean(this.dryRun)) {
            return null;
        }

        FundMaster fund = createBaseFundEntity(item);

        List<BenchmarkMaster> matchedBenchmarks = resolveBenchmarksForFund(item);
        List<FundBenchmarkAssociation> associations = new ArrayList<>();

        if (matchedBenchmarks != null && !matchedBenchmarks.isEmpty()) {
            boolean isFirst = true;
            for (BenchmarkMaster bm : matchedBenchmarks) {
                // Set the first benchmark as primary on fund_master
                if (isFirst) {
                    fund.setBenchmarkId(bm.getBenchmarkId());
                    fund.setBenchmarkName(bm.getBenchmarkName());
                }

                // Add to bridge associations
                associations.add(new FundBenchmarkAssociation(
                        item.getSeriesId(),
                        bm.getBenchmarkId(),
                        bm.getBenchmarkName(),
                        isFirst,
                        item.getAccessionNumber()
                ));
                isFirst = false;
            }
        } else {
            // Domain Fallback: newly incepted funds or filings without quantitative returns
            log.debug("No benchmark disclosed for filing [{}] and series [{}]. Applying fallback domain values.",
                    item.getAccessionNumber(), item.getSeriesId());

            fund.setBenchmarkId(this.defaultBenchmarkId);
            fund.setBenchmarkName(DEFAULT_BENCHMARK_NAME);

            associations.add(new FundBenchmarkAssociation(
                    item.getSeriesId(),
                    this.defaultBenchmarkId,
                    DEFAULT_BENCHMARK_NAME,
                    true,
                    item.getAccessionNumber()
            ));
        }

        return new FundImportPayload(fund, associations);
    }

    private FundMaster createBaseFundEntity(RawFundSource item) {
        FundMaster fund = new FundMaster();
        String cadence = resolveReportingCadence(item.getFormType());
        String classifiedStrategy = evaluateStrategy(item.getDisclosureValue());
        fund.setFundFamily(item.getFundFamily());
        // Use the fund name from the tag mapping if available; otherwise, fallback to the fund family
        fund.setFundName(this.fundNamesByTagMap.getOrDefault(item.getSeriesId().strip(), item.getFundFamily()));
        fund.setSeriesId(item.getSeriesId().strip());
        fund.setCik(item.getCik());
        fund.setAccessionNumber(item.getAccessionNumber());
        fund.setPrimaryTicker(item.getTicker());
        fund.setStrategyType(classifiedStrategy);
        fund.setCreatedBy(this.executor);
        fund.setUpdatedBy(this.executor);
        fund.setReportingCadence(cadence);
        fund.setHasDailyPricing(Boolean.TRUE);
        fund.setHasQuarterlyHoldings(Boolean.TRUE);
        resolveStrategyNarrativeAndInvestmentObjective(item, fund);
        return fund;
    }

    private void resolveStrategyNarrativeAndInvestmentObjective(RawFundSource item, FundMaster fund) {
        if (item.getDisclosureTag().equalsIgnoreCase(
                Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag())
                || item.getDisclosureTag().equalsIgnoreCase(
                Tags.INVESTMENT_STRATEGY_TEXT_BLOCK.getTag())) {
            fund.setStrategyNarrative(item.getDisclosureValue());
        } else if (item.getDisclosureTag().equalsIgnoreCase(Tags.OBJECTIVE_PRIMARY_TEXT_BLOCK.getTag())
                || item.getDisclosureTag().equalsIgnoreCase(Tags.OBJECTIVE_SECONDARY_TEXT_BLOCK.getTag())) {
            fund.setInvestmentObjective(item.getDisclosureValue());
        }
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

    private List<BenchmarkMaster> resolveBenchmarksForFund(RawFundSource item) {
        String accessionNumber = item.getAccessionNumber() != null ? item.getAccessionNumber().trim() : null;

        return accessionNumber != null
                ? this.benchmarksByAccessionMap.getOrDefault(accessionNumber, Collections.emptyList())
                : Collections.emptyList();
    }

    private String resolveReportingCadence(String formType) {
        if (formType == null || formType.isBlank()) {
            return ANNUAL_CADENCE;
        }
        String normalized = formType.trim().toUpperCase();
        if (normalized.startsWith("485") || normalized.contains("10-K")) {
            return ANNUAL_CADENCE;
        } else if (normalized.contains("497")) {
            return "SUPPLEMENTAL";
        }
        return ANNUAL_CADENCE;
    }
}