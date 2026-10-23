package funds.processor;

import ch.qos.logback.core.util.StringUtil;
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
    private final String executor;
    private final AnalyticsRepository analyticsRepository;

    private Pattern boilerplatePattern;
    private List<BenchmarkCandidate> rules = Collections.emptyList();
    private final Map<Integer, Pattern> patternCache = new HashMap<>();

    public BenchmarkItemProcessor(
            AnalyticsRepository analyticsRepository,
            @Value("${benchmark.regex:}") String benchmarkRegex,
            // Regex targeting standard Form N-1A narrative benchmark comparative clauses
            @Value("${benchmark.narrative.regex}") String benchmarkNarrativeRegex,
            // Strip terminal Member or [Member] variations in pure Java with regex
            @Value("${benchmark.suffix-regex}") String benchmarkSuffixRegex,
            @Value("#{jobParameters['executor']}") String executor
    ) {
        this.analyticsRepository = analyticsRepository;
        this.benchmarkRegex = benchmarkRegex;
        this.executor = executor;
        this.benchmarkNarrativeRegex = benchmarkNarrativeRegex;
        this.benchmarkSuffixRegex = benchmarkSuffixRegex;
    }

    @BeforeStep
    public void setup(StepExecution stepExecution) {
        try {
            if (benchmarkRegex != null && !benchmarkRegex.isBlank()) {
                this.boilerplatePattern = Pattern.compile(this.benchmarkRegex, Pattern.CASE_INSENSITIVE);
            } else {
                this.boilerplatePattern = Pattern.compile("\\s*\\(reflects no deduction for [^)]*\\)", Pattern.CASE_INSENSITIVE);
            }
        } catch (PatternSyntaxException pse) {
            log.error("Invalid benchmark.regex property [{}]. Defaulting to standard SEC cleaner.", this.benchmarkRegex, pse);
            this.boilerplatePattern = Pattern.compile("\\s*\\(reflects no deduction for [^)]*\\)", Pattern.CASE_INSENSITIVE);
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

        // 1. Resolve raw candidate via Java fallback precedence
        Optional<String> candidateOpt = extractCandidate(source);
        if (candidateOpt.isEmpty()) {
            return null;
        }

        String rawCandidate = candidateOpt.get();

        // 2. If this candidate originated from a TextBlock disclosure, extract the benchmark name entity
        if (isTextBlockDisclosure(source)) {
            rawCandidate = extractEntityFromNarrative(rawCandidate);
            if (rawCandidate == null || rawCandidate.isBlank()) {
                // Could not extract a specific benchmark from narrative prose; skip record
                return null;
            }
        }

        // 3. Filter non-benchmark measure strings
        String lower = rawCandidate.toLowerCase();
        if (lower.contains("aftertax") || lower.contains("returnbeforetax")) {
            return null;
        }

        // 4. Strip trailing Member / [Member]
        String stripped = Pattern.compile(benchmarkSuffixRegex).matcher(rawCandidate).replaceAll("").trim();

        // 5. Clean boilerplate text
        String cleaned = this.boilerplatePattern.matcher(stripped).replaceAll("").trim();
        if (cleaned.isBlank()) {
            return null;
        }

        // 6. Safety Guard: Benchmark name cannot exceed 255 chars (prevents B-tree index row limit overflow)
        if (cleaned.length() > 255) {
            log.warn("Discarding candidate exceeding max length ({} chars): [{}]", cleaned.length(), cleaned.substring(0, 100));
            return null;
        }

        // 7. Match Provider & Type via rules table
        String matchedProvider = "Other / Self-Indexed";
        String matchedType = "Category / Secondary";

        for (BenchmarkCandidate rule : this.rules) {
            Pattern pattern = patternCache.get(rule.getRuleId());
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

    private boolean isTextBlockDisclosure(RawBenchmarkSource source) {
        String tag = source.getRawMeasure();
        return tag != null && (tag.endsWith("TextBlock") || tag.contains("Narrative"));
    }

    private String extractEntityFromNarrative(String prose) {
        // Strip HTML markup and normalize whitespace
        String cleanProse = prose.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();

        Matcher matcher = Pattern.compile(this.benchmarkNarrativeRegex, Pattern.CASE_INSENSITIVE).matcher(cleanProse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}