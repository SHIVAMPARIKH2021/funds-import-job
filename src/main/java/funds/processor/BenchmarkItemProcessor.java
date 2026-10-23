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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Component
@StepScope
public class BenchmarkItemProcessor implements ItemProcessor<RawBenchmarkSource, BenchmarkMaster> {

    private final String benchmarkRegex;
    private final String executor;
    private final AnalyticsRepository analyticsRepository;

    private Pattern boilerplatePattern;
    private List<BenchmarkCandidate> rules = Collections.emptyList();
    private final Map<Integer, Pattern> patternCache = new HashMap<>();

    // Strip terminal Member or [Member] variations in pure Java
    private static final Pattern MEMBER_SUFFIX_PATTERN =
            Pattern.compile("(?i)(\\s*\\[Member\\]|Member)$");

    public BenchmarkItemProcessor(
            AnalyticsRepository analyticsRepository,
            @Value("${benchmark.regex:}") String benchmarkRegex,
            @Value("#{jobParameters['executor']}") String executor
    ) {
        this.analyticsRepository = analyticsRepository;
        this.benchmarkRegex = benchmarkRegex;
        this.executor = executor;
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

        // 2. Filter out non-benchmark measure strings in Java
        String lower = rawCandidate.toLowerCase();
        if (lower.contains("aftertax") || lower.contains("returnbeforetax")) {
            return null;
        }

        // 3. Strip trailing "[Member]" or "Member"
        String stripped = MEMBER_SUFFIX_PATTERN.matcher(rawCandidate).replaceAll("").trim();

        // 4. Clean boilerplate text (e.g. "(reflects no deduction for fees...")
        String cleaned = this.boilerplatePattern.matcher(stripped).replaceAll("").trim();
        if (cleaned.isBlank()) {
            return null;
        }

        // 5. Categorize Provider and Type via rules table
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
}