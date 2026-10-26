package funds.processor;

import ch.qos.logback.core.util.StringUtil;
import funds.constants.Tags;
import funds.model.BenchmarkCandidate;
import funds.model.BenchmarkMaster;
import funds.model.RawBenchmarkSource;
import funds.repository.AnalyticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Component
@StepScope
public class BenchmarkItemProcessor implements ItemProcessor<RawBenchmarkSource, BenchmarkMaster> {

    private final String benchmarkRegex;
    private final String benchmarkNarrativeRegex;
    private final String benchmarkSuffixRegex;
    private final String transitionRegex;
    private final String executor;
    private final AnalyticsRepository analyticsRepository;

    private Pattern boilerplatePattern;
    private Pattern suffixPattern;
    private Pattern benchmarkNarrativePattern;
    private Pattern transitionPattern;
    private List<BenchmarkCandidate> rules = Collections.emptyList();
    private final Map<Integer, Pattern> patternCache = new HashMap<>();

    public BenchmarkItemProcessor(
            AnalyticsRepository analyticsRepository,
            @Value("${benchmark.regex:}") String benchmarkRegex,
            // Regex targeting standard Form N-1A narrative benchmark comparative clauses
            @Value("${benchmark.narrative.regex}") String benchmarkNarrativeRegex,
            // Strip terminal Member or [Member] variations in pure Java with regex
            @Value("${benchmark.suffix-regex}") String benchmarkSuffixRegex,
            @Value("${benchmark.transition-regex}") String transitionRegex,
            @Value("#{jobParameters['executor']}") String executor
    ) {
        this.analyticsRepository = analyticsRepository;
        this.benchmarkRegex = benchmarkRegex;
        this.executor = executor;
        this.benchmarkNarrativeRegex = benchmarkNarrativeRegex;
        this.transitionRegex = transitionRegex;
        this.benchmarkSuffixRegex = benchmarkSuffixRegex;
    }

    @BeforeStep
    public void setup(StepExecution stepExecution) {
        try {
            if (benchmarkRegex != null && !benchmarkRegex.isBlank()) {
                this.boilerplatePattern = Pattern.compile(this.benchmarkRegex, Pattern.CASE_INSENSITIVE);
            }
            if (benchmarkSuffixRegex != null && !benchmarkSuffixRegex.isBlank()) {
                this.suffixPattern = Pattern.compile(this.benchmarkSuffixRegex, Pattern.CASE_INSENSITIVE);
            }
            if (benchmarkNarrativeRegex != null && !benchmarkNarrativeRegex.isBlank()) {
                this.benchmarkNarrativePattern = Pattern.compile(this.benchmarkNarrativeRegex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            }
            if (transitionRegex != null && !transitionRegex.isBlank()) {
                this.transitionPattern = Pattern.compile(this.transitionRegex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            }
        } catch (PatternSyntaxException pse) {
            log.error("Invalid regex syntax in benchmark configuration. Check application properties.", pse);
        }

        this.rules = analyticsRepository.fetchBenchmarkProviderRules();
        this.patternCache.clear();

        if (this.rules != null) {
            for (BenchmarkCandidate rule : this.rules) {
                if (rule.getPattern() != null && !rule.getPattern().isBlank()) {
                    try {
                        this.patternCache.put(
                                rule.getRuleId(),
                                Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                        );
                    } catch (PatternSyntaxException pse) {
                        log.error("Corrupted regex syntax in ruleId [{}], pattern [{}]. Rule will be skipped.",
                                rule.getRuleId(), rule.getPattern(), pse);
                    }
                }
            }
        }

        log.info("Initialized {} benchmark provider rules for step '{}'",
                this.rules != null ? this.rules.size() : 0, stepExecution.getStepName());
    }

    @Override
    public BenchmarkMaster process(RawBenchmarkSource source) {
        if (source == null) {
            return null;
        }

        // 1. Resolve raw candidate from numeric labels/measures
        Optional<String> candidateOpt = extractCandidate(source);
        String cleaned = null;

        if (candidateOpt.isPresent()) {
            String rawCandidate = candidateOpt.get();

            // Exclude non-benchmark internal fund adjustments
            String lower = rawCandidate.toLowerCase();
            if (!lower.contains("aftertax") && !lower.contains("returnbeforetax")) {
                // Strip trailing Member / [Member] using pre-compiled pattern
                String stripped = stripSuffix(rawCandidate);

                // Clean boilerplate text using pre-compiled pattern
                String candCleaned = this.boilerplatePattern != null
                        ? this.boilerplatePattern.matcher(stripped).replaceAll("").trim()
                        : stripped;

                if (!candCleaned.isBlank()) {
                    cleaned = candCleaned;
                }
            }
        }

        // 2. Fallback to Text Disclosures ONLY IF numeric facts yielded no benchmark
        if (cleaned == null || cleaned.isBlank()) {
            List<RawBenchmarkSource> textDisclosureSources = analyticsRepository.findUnresolvedBenchmarksFromTextFacts(
                    List.of(Tags.MARKET_INDEX_PERFORMANCE_COLUMN_HEADING.getTag(),
                            Tags.INDEX_NO_DEDUCTION_FOR_FEES_EXPENSES_TAXES.getTag(),
                            Tags.PERFORMANCE_TABLE_NARRATIVE_TEXT_BLOCK.getTag(),
                            Tags.PERFORMANCE_NARRATIVE_TEXT_BLOCK.getTag(),
                            Tags.PERFORMANCE_TABLE_HEADING.getTag()),
                    source.getAccessionNumber()
            );

            if (textDisclosureSources != null && !textDisclosureSources.isEmpty()) {
                for (RawBenchmarkSource textSource : textDisclosureSources) {
                    String textContent = !StringUtil.isNullOrEmpty(textSource.getRawMeasure())
                            ? textSource.getRawMeasure()
                            : textSource.getStdLabel();

                    if (textContent != null && !textContent.isBlank()) {
                        boolean isNarrative = isTextBlockDisclosure(textSource);

                        // If it is a text block, strictly extract the index entity; never fall back to raw narrative
                        String extracted = isNarrative
                                ? extractEntityFromNarrative(textContent)
                                : textContent;

                        if (extracted != null && !extracted.isBlank()) {
                            String stripped = stripSuffix(extracted);
                            String candCleaned = this.boilerplatePattern != null
                                    ? this.boilerplatePattern.matcher(stripped).replaceAll("").trim()
                                    : stripped;

                            // Filter out generic table headings and intro paragraphs
                            String lower = candCleaned.toLowerCase();
                            boolean isGenericBoilerplate = lower.contains("average annual total return")
                                    || lower.startsWith("the bar chart")
                                    || lower.startsWith("the information")
                                    || lower.contains("table heading");

                            if (!candCleaned.isBlank() && candCleaned.length() <= 100 && !isGenericBoilerplate) {
                                cleaned = candCleaned;
                                break; // Stop only when a genuine index title has been resolved
                            }
                        }
                    }
                }
            }
        }

        // If neither numeric facts nor text disclosures yielded a benchmark name, skip
        if (cleaned == null || cleaned.isBlank()) {
            return null;
        }

        // 3. Match Provider & Type via pre-compiled rules table
        String matchedProvider = "Other / Self-Indexed";
        String matchedType = "Category / Secondary";

        for (BenchmarkCandidate rule : this.rules) {
            Pattern pattern = this.patternCache.get(rule.getRuleId());
            if (pattern != null && pattern.matcher(cleaned).find()) {
                matchedProvider = rule.getProviderName();
                matchedType = rule.getDefaultType();
                break;
            }
        }

        BenchmarkMaster benchmarkMaster = new BenchmarkMaster();
        benchmarkMaster.setBenchmarkName(cleaned);
        benchmarkMaster.setBenchmarkProvider(matchedProvider);
        benchmarkMaster.setBenchmarkType(matchedType);
        benchmarkMaster.setCreatedBy(this.executor);
        benchmarkMaster.setUpdatedBy(this.executor);
        benchmarkMaster.setAccessionNumber(source.getAccessionNumber() != null ? source.getAccessionNumber().trim() : null);

        return benchmarkMaster;
    }

    private Optional<String> extractCandidate(RawBenchmarkSource source) {
        if (!StringUtil.isNullOrEmpty(source.getTerseLabel())) {
            return Optional.of(source.getTerseLabel().trim());
        }
        if (!StringUtil.isNullOrEmpty(source.getStdLabel())) {
            return Optional.of(source.getStdLabel().trim());
        }
        if (!StringUtil.isNullOrEmpty(source.getVerboseLabel())) {
            return Optional.of(source.getVerboseLabel().trim());
        }
        if (!StringUtil.isNullOrEmpty(source.getRawMeasure())) {
            return Optional.of(source.getRawMeasure().trim());
        }
        return Optional.empty();
    }

    private String stripSuffix(String text) {
        if (text == null) return "";
        return this.suffixPattern != null
                ? this.suffixPattern.matcher(text).replaceAll("").trim()
                : text.trim();
    }

    private boolean isTextBlockDisclosure(RawBenchmarkSource source) {
        String label = source.getStdLabel();
        String measure = source.getRawMeasure();
        return (label != null && (label.endsWith("TextBlock") || label.contains("Narrative")))
                || (measure != null && (measure.endsWith("TextBlock") || measure.contains("Narrative")));
    }

    private String extractEntityFromNarrative(String prose) {
        if (StringUtil.isNullOrEmpty(prose)) {
            return null;
        }

        // Strip HTML markup and collapse whitespace upfront
        String cleanProse = prose.replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleanProse.isBlank()) {
            return null;
        }

        // Priority 1: Transition statement match (changed from A to B)
        if (this.transitionPattern != null) {
            Matcher transitionMatcher = this.transitionPattern.matcher(cleanProse);
            if (transitionMatcher.find()) {
                return transitionMatcher.group(1).trim();
            }
        }

        // Priority 2: Standard comparative clause match
        if (this.benchmarkNarrativePattern != null) {
            Matcher matcher = this.benchmarkNarrativePattern.matcher(cleanProse);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return null;
    }




}