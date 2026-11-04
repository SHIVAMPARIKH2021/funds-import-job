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
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
@StepScope
public class FundsItemProcessor implements ItemProcessor<RawFundSource, FundImportPayload> {

    private static final String ANNUAL_CADENCE = "ANNUAL";
    private static final String DEFAULT_BENCHMARK_NAME = "Not Disclosed / New Inception";

    private final String executor;
    private final String dryRun;
    private final String activeStrategyRegex;
    private final String passiveStrategyRegex;
    private final String fundIndexRegex;
    private final String fundBenchmarkProviderRegex;
    private final int defaultBenchmarkId;
    private final AnalyticsRepository analyticsRepository;
    private final Map<Integer, Pattern> complianceRuleMap = new HashMap<>();
    private List<ComplianceRules> complianceRules = Collections.emptyList();
    private Set<String> seriesWithPrimaryBenchmark = Collections.emptySet();
    // --- Pre-compiled Patterns for Primary Benchmark Scoring ---
    private static final Pattern BENCHMARK_BOILERPLATE_PATTERN = Pattern.compile(
            "(?i)\\s*[\\(\\[\\*]?\\s*(?:reflects?\\s+no\\s+deductions?|no\\s+deductions?|reflects?\\s+no\\s+fees?|gross\\s+of\\s+taxes?|net\\s+of\\s+taxes?).*"
    );

    private static final Pattern CLEAN_PUNCTUATION_PATTERN = Pattern.compile(
            "[*,\\[\\]()./\\-—_\"'%&]"
    );

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "of", "and", "in", "for", "to", "a", "an", "index", "total", "return", "fund", "series", "class", "trust"
    );

    // Grouped 1:N cache loaded once per step: accession_number -> List<BenchmarkMaster>
    private Map<String, List<BenchmarkMaster>> benchmarksByAccessionAndSeriesMap = Collections.emptyMap();

    private Map<String, String> fundNamesByTagMap = Collections.emptyMap();

    public FundsItemProcessor(AnalyticsRepository analyticsRepository,
                              @Value("#{jobParameters['executor']}") String executor,
                              @Value("#{jobParameters['dryrun']}") String dryRun,
                              @Value("${benchmark.default.id:0}") int defaultBenchmarkId,
                              @Value("${fund.strategy.active.regex}") String activeStrategyRegex,
                              @Value("${fund.strategy.passive.regex}") String passiveStrategyRegex,
                              @Value("${fund.index.regex}") String fundIndexRegex,
                              @Value("${benchmark.provider.regex}") String fundBenchmarkProviderRegex
    ) {
        this.analyticsRepository = analyticsRepository;
        this.executor = executor;
        this.dryRun = dryRun;
        this.defaultBenchmarkId = defaultBenchmarkId;
        this.activeStrategyRegex = activeStrategyRegex;
        this.passiveStrategyRegex = passiveStrategyRegex;
        this.fundIndexRegex = fundIndexRegex;
        this.fundBenchmarkProviderRegex = fundBenchmarkProviderRegex;
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
        this.benchmarksByAccessionAndSeriesMap.clear();
        this.benchmarksByAccessionAndSeriesMap = analyticsRepository.mapfindBenchmarksGroupedByAccessionAndSeries();

        this.fundNamesByTagMap.clear();
        this.fundNamesByTagMap = analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());

        log.info("Initialized {} compliance rules and {} distinct accession benchmarks for step '{}'",
                this.complianceRules.size(), this.benchmarksByAccessionAndSeriesMap.size(), stepExecution.getStepName());
        this.seriesWithPrimaryBenchmark = analyticsRepository.findSeriesWithActivePrimaryBenchmark();
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
            BenchmarkMaster primaryBenchmark = selectPrimaryBenchmark(matchedBenchmarks, item, fund);
            fund.setBenchmarkId(primaryBenchmark.getBenchmarkId());
            fund.setBenchmarkName(primaryBenchmark.getBenchmarkName());
            for (BenchmarkMaster bm : matchedBenchmarks) {
                boolean isPrimary = bm.getBenchmarkId().equals(primaryBenchmark.getBenchmarkId());
                associations.add(new FundBenchmarkAssociation(
                        stripeSeries(item.getSeriesId()),
                        bm.getBenchmarkId(),
                        bm.getBenchmarkName(),
                        isPrimary,
                        stripeAccessionNumber(item.getAccessionNumber())
                ));
            }
        } else {
            // Domain Fallback: newly incepted funds or filings without quantitative returns
            log.debug("No benchmark disclosed for filing [{}] and series [{}]. Applying fallback domain values.",
                    item.getAccessionNumber(), item.getSeriesId());

            String cleanSeriesId = item.getSeriesId().strip();
            boolean markAsPrimary = !this.seriesWithPrimaryBenchmark.contains(cleanSeriesId);

            fund.setBenchmarkId(this.defaultBenchmarkId);
            fund.setBenchmarkName(DEFAULT_BENCHMARK_NAME);

            associations.add(new FundBenchmarkAssociation(
                    cleanSeriesId,
                    this.defaultBenchmarkId,
                    DEFAULT_BENCHMARK_NAME,
                    markAsPrimary,
                    item.getAccessionNumber()
            ));
        }

        log.info("Fund Processed: series={}, accessionNumber={}", fund.getSeriesId(), fund.getAccessionNumber());
        return new FundImportPayload(fund, associations);
    }

    private FundMaster createBaseFundEntity(RawFundSource item) {
        FundMaster fund = new FundMaster();
        String fundName = fetchFundName(item.getSeriesId(), item.getFundFamily());
        String cadence = resolveReportingCadence(item.getFormType());
        String classifiedStrategy = evaluateStrategy(fundName, item.getInvestmentObjective(), item.getStrategyNarrative());
        fund.setFundFamily(item.getFundFamily());
        // Use the fund name from the tag mapping if available; otherwise, fallback to the fund family
        fund.setFundName(fundName);
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
        fund.setInvestmentObjective(item.getInvestmentObjective());
        fund.setStrategyNarrative(item.getStrategyNarrative());
        return fund;
    }

    private String evaluateStrategy(String fundName, String objective, String narrative) {
        var UNKNOWN_TYPE = "UNKNOWN";

        // 1. Tier 1: Fast Heuristic on Legal Fund Name
        if (fundName != null && !fundName.isBlank()) {
            // Strip periods and unescape HTML/punctuation for consistent matching
            String cleanName = fundName.replaceAll("\\.", "").toLowerCase().trim();

            // Check 1A: Explicit Active branding takes immediate priority
            // Handles cases like "JPMorgan Active Growth ETF" or "Active S&P ETF"
            if (cleanName.matches(activeStrategyRegex)) {
                return "ACTIVE";
            }

            // Check 1B: Explicit Index / Passive indicators in Name
            if (cleanName.matches(this.passiveStrategyRegex)) {
                return "PASSIVE";
            }

            boolean hasIndexMarker = cleanName.matches(this.fundIndexRegex);
            boolean hasProvider = cleanName.matches(this.fundBenchmarkProviderRegex);

            if (hasIndexMarker && hasProvider) {
                return "PASSIVE";
            }
        }

        // 2. Tier 2: Combine Objective and Strategy Narrative
        StringBuilder combinedText = new StringBuilder();
        if (objective != null && !objective.isBlank()) {
            combinedText.append(objective).append(" ");
        }
        if (narrative != null && !narrative.isBlank()) {
            combinedText.append(narrative);
        }

        String fullText = combinedText.toString().trim();
        if (fullText.isEmpty()) {
            return UNKNOWN_TYPE;
        }

        // 3. Tier 3: Evaluate Dynamic Compliance Rules Engine (Negations -> Passive -> Active)[cite: 2]
        for (ComplianceRules rule : this.complianceRules) {
            boolean matches;
            if (Boolean.TRUE.equals(rule.getIsRegex())) {
                Pattern pattern = this.complianceRuleMap.get(rule.getRuleId());
                matches = pattern != null && pattern.matcher(fullText).find();
            } else {
                matches = fullText.toLowerCase().contains(rule.getPattern().toLowerCase());
            }

            if (matches) {
                return rule.getTargetStrategy();
            }
        }

        return UNKNOWN_TYPE;
    }

    private List<BenchmarkMaster> resolveBenchmarksForFund(RawFundSource item) {
        String accessionNumber = stripeAccessionNumber(item.getAccessionNumber());
        String series = stripeSeries(item.getSeriesId());
        String compositeKey = accessionNumber + "_" + series;

        return accessionNumber != null
                ? this.benchmarksByAccessionAndSeriesMap.getOrDefault(compositeKey, Collections.emptyList())
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

    /**
     * Scoring Tie-Breaker method for benchmark selection
     */
    private BenchmarkMaster selectPrimaryBenchmark(List<BenchmarkMaster> candidates, RawFundSource item, FundMaster fund) {
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }

        // Normalize text from both the item and the populated fund entity
        String normObjective = normalizeText(fund.getInvestmentObjective());
        String normFundName = normalizeText(fetchFundName(item.getSeriesId(), item.getFundFamily()));
        String normNarrative = normalizeText(item.getStrategyNarrative());

        BenchmarkMaster bestMatch = candidates.getFirst();
        int highestScore = -1;

        for (BenchmarkMaster bm : candidates) {
            String rawBmName = bm.getBenchmarkName();
            if (rawBmName == null || rawBmName.isBlank()) {
                continue;
            }

            // Strip disclaimer boilerplate
            String cleanBm = BENCHMARK_BOILERPLATE_PATTERN.matcher(rawBmName).replaceAll("");
            String normBm = normalizeText(cleanBm);

            if (normBm.length() < 3) {
                normBm = normalizeText(rawBmName);
            }

            int objectiveScore = scoreCandidateMatch(normBm, normObjective) * 3; // Weight x3
            int fundNameScore = scoreCandidateMatch(normBm, normFundName) * 2;   // Weight x2
            int narrativeScore = scoreCandidateMatch(normBm, normNarrative);     // Weight x1

            int totalScore = objectiveScore + fundNameScore + narrativeScore;

            if (totalScore > highestScore) {
                highestScore = totalScore;
                bestMatch = bm;
            }
        }

        return bestMatch;
    }

    private int scoreCandidateMatch(String normBm, String targetText) {
        if (normBm.isBlank() || targetText.isBlank()) {
            return 0;
        }

        // Tier 1: Exact full phrase match
        if (targetText.contains(normBm)) {
            return 100;
        }

        // Tier 2: Phrase match omitting the word "index"
        String withoutIndex = normBm.replaceAll("\\bindex\\b", "").replaceAll("\\s+", " ").trim();
        if (withoutIndex.length() >= 3 && targetText.contains(withoutIndex)) {
            return 85;
        }

        // Tier 3: Token overlap
        String[] tokens = normBm.split("\\s+");
        List<String> keyTokens = new ArrayList<>();
        for (String t : tokens) {
            if (!STOP_WORDS.contains(t) && t.length() > 1) {
                keyTokens.add(t);
            }
        }

        if (keyTokens.isEmpty()) {
            return 0;
        }

        int matchedCount = 0;
        boolean hasDistinctiveNumber = false;

        for (String token : keyTokens) {
            Pattern wordPattern = Pattern.compile("\\b" + Pattern.quote(token) + "\\b");
            if (wordPattern.matcher(targetText).find()) {
                matchedCount++;
                if (!hasDistinctiveNumber && token.matches("\\d+")) {
                    hasDistinctiveNumber = true;
                }
            }
        }

        double ratio = (double) matchedCount / keyTokens.size();
        if (ratio >= 0.75) {
            return (int) (60 * ratio) + (hasDistinctiveNumber ? 20 : 0);
        } else if (ratio >= 0.50) {
            return (int) (30 * ratio) + (hasDistinctiveNumber ? 15 : 0);
        }

        return 0;
    }

    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // Unescape &amp;, &quot;, &#160; etc. using Spring's built-in utility
        String unescaped = HtmlUtils.htmlUnescape(text.toLowerCase());
        unescaped = unescaped.replaceAll("<[^>]+>", " ");
        String stripped = CLEAN_PUNCTUATION_PATTERN.matcher(unescaped).replaceAll(" ");
        return WHITESPACE_PATTERN.matcher(stripped).replaceAll(" ").trim();
    }

    private String stripeAccessionNumber(String accessionNumber) {
        return accessionNumber != null ? accessionNumber.trim() : null;
    }

    private String stripeSeries(String seriesId) {
        return seriesId != null ? seriesId.strip() : "";
    }

    private String fetchFundName(String series, String fundFamily) {
        var strippedSeries = stripeSeries(series);
        return this.fundNamesByTagMap.getOrDefault(strippedSeries, fundFamily);
    }

}