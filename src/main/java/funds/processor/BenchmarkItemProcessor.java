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

import java.nio.charset.StandardCharsets;
import java.util.*;
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

    public BenchmarkItemProcessor(
            AnalyticsRepository analyticsRepository,
            @Value("${benchmark.regex}") String benchmarkRegex,
            @Value("#{jobParameters['executor']}") String executor
    ) {
        this.analyticsRepository = analyticsRepository;
        this.benchmarkRegex = benchmarkRegex;
        this.executor = executor;
    }

    @BeforeStep
    public void setup(StepExecution stepExecution) {
        // 1. Compile boilerplate cleanup regex once
        try {
            this.boilerplatePattern = Pattern.compile(this.benchmarkRegex, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException pse) {
            log.error("Invalid benchmark.regex property [{}]. Defaulting to standard SEC cleaner.", this.benchmarkRegex, pse);
            this.boilerplatePattern = Pattern.compile("\\s*\\(reflects no deduction for [^)]*\\)", Pattern.CASE_INSENSITIVE);
        }

        // 2. Assign directly to the instance field (avoid local variable shadowing)
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

        log.info("Initialized {} benchmark provider rules ({} patterns compiled successfully) for step '{}'",
                this.rules != null ? this.rules.size() : 0, this.patternCache.size(), stepExecution.getStepName());
    }

    @Override
    public BenchmarkMaster process(RawBenchmarkSource benchmark) {
        if (StringUtil.isNullOrEmpty(benchmark.getBenchmarkName())) {
            return null; // Skip invalid records
        }

        // 1. Clean boilerplate text using the pre-compiled pattern
        String cleaned = this.boilerplatePattern.matcher(benchmark.getBenchmarkName()).replaceAll("").trim();
        if (cleaned.isBlank()) {
            return null;
        }

        // 2. Evaluate against compiled regex rules by priority
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

        // 3. Assemble entity with deterministic Type-3 UUID from the clean name
        BenchmarkMaster benchmarkMaster = new BenchmarkMaster();
        benchmarkMaster.setBenchmarkId(UUID.nameUUIDFromBytes(cleaned.getBytes(StandardCharsets.UTF_8)));
        benchmarkMaster.setBenchmarkName(cleaned);
        benchmarkMaster.setBenchmarkProvider(matchedProvider);
        benchmarkMaster.setBenchmarkType(matchedType);
        benchmarkMaster.setCreatedBy(this.executor);
        benchmarkMaster.setAccessionNumber(benchmark.getAccessionNumber().trim());

        return benchmarkMaster;
    }
}