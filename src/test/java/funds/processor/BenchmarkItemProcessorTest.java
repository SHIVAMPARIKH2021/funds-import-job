package funds.processor;

import funds.model.BenchmarkCandidate;
import funds.model.BenchmarkMaster;
import funds.model.RawBenchmarkSource;
import funds.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepExecution;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BenchmarkItemProcessorTest {

    private static final String DEFAULT_BOILERPLATE_REGEX = "\\s*\\(reflects no deduction for [^)]*\\)";

    @Mock
    private AnalyticsRepository analyticsRepository;

    private BenchmarkItemProcessor benchmarkItemProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        benchmarkItemProcessor = buildProcessor("(reflects no deduction for [^)]*)");
        // @BeforeStep is a Spring Batch lifecycle callback and is never invoked in plain
        // Mockito unit tests; stub the repository and call setup(...) explicitly instead.
        when(analyticsRepository.fetchBenchmarkProviderRules()).thenReturn(Collections.emptyList());
        benchmarkItemProcessor.setup(mock(StepExecution.class));
    }

    private BenchmarkItemProcessor buildProcessor(String benchmarkRegex) {
        return new BenchmarkItemProcessor(analyticsRepository, benchmarkRegex,"", "","TEST", "");
    }

    private BenchmarkCandidate rule(Integer ruleId, String pattern, String providerName, String defaultType) {
        BenchmarkCandidate candidate = new BenchmarkCandidate();
        candidate.setRuleId(ruleId);
        candidate.setPattern(pattern);
        candidate.setProviderName(providerName);
        candidate.setDefaultType(defaultType);
        return candidate;
    }

    private RawBenchmarkSource source(String benchmarkName, String accessionNumber) {
        RawBenchmarkSource source = new RawBenchmarkSource();
        source.setStdLabel(benchmarkName);
        source.setAccessionNumber(accessionNumber);
        return source;
    }

    @Test
    void testProcess_SkipsInvalidRecordWithNullBenchmarkName() {
        assertNull(benchmarkItemProcessor.process(source(null, "ACC12345")));
    }

    @Test
    void testProcess_SkipsInvalidRecordWithEmptyBenchmarkName() {
        assertNull(benchmarkItemProcessor.process(source("   ", "ACC12345")));
    }

    @Test
    void testProcess_ReturnsValidBenchmarkMaster() {
        BenchmarkMaster result = benchmarkItemProcessor.process(source("S&P 500", "ACC12345"));

        assertNotNull(result);
        assertEquals("S&P 500", result.getBenchmarkName());
        assertEquals("Other / Self-Indexed", result.getBenchmarkProvider());
        assertEquals("Category / Secondary", result.getBenchmarkType());
    }

    @Test
    void testProcess_SetsCreatedByAndTrimsAccessionNumber() {
        BenchmarkMaster result = benchmarkItemProcessor.process(source("S&P 500", " ACC12345 "));

        assertNotNull(result);
        assertEquals("TEST", result.getCreatedBy());
        assertEquals("ACC12345", result.getAccessionNumber());
    }

    @Test
    void testProcess_RemovesBoilerplateText() {
        BenchmarkItemProcessor processor = buildProcessor(DEFAULT_BOILERPLATE_REGEX);
        processor.setup(mock(StepExecution.class));

        BenchmarkMaster result = processor.process(
                source("S&P 500 Index (reflects no deduction for taxes)", "ACC12345"));

        assertNotNull(result);
        assertEquals("S&P 500 Index", result.getBenchmarkName());
    }

    @Test
    void testProcess_BoilerplateCleanupIsCaseInsensitive() {
        BenchmarkItemProcessor processor = buildProcessor(DEFAULT_BOILERPLATE_REGEX);
        processor.setup(mock(StepExecution.class));

        BenchmarkMaster result = processor.process(
                source("S&P 500 (REFLECTS NO DEDUCTION FOR TAXES)", "ACC12345"));

        assertNotNull(result);
        assertEquals("S&P 500", result.getBenchmarkName());
    }

    @Test
    void testProcess_ReturnsNullWhenNameIsOnlyBoilerplate() {
        BenchmarkItemProcessor processor = buildProcessor(DEFAULT_BOILERPLATE_REGEX);
        processor.setup(mock(StepExecution.class));

        assertNull(processor.process(source("  (reflects no deduction for taxes)  ", "ACC12345")));
    }

    @Test
    void testProcess_MatchesFirstMatchingRuleByPriorityOrder() {
        // Rules are evaluated in list order (repository orders them by priority)
        when(analyticsRepository.fetchBenchmarkProviderRules()).thenReturn(Arrays.asList(
                rule(1, "S&P.*", "S&P Global", "INDEX"),
                rule(2, ".*", "Other Provider", "CUSTOM")));
        benchmarkItemProcessor.setup(mock(StepExecution.class));

        BenchmarkMaster result = benchmarkItemProcessor.process(source("S&P 500", "ACC12345"));

        assertNotNull(result);
        assertEquals("S&P Global", result.getBenchmarkProvider());
        assertEquals("INDEX", result.getBenchmarkType());
    }

    @Test
    void testProcess_FallsBackToDefaultsWhenNoRuleMatches() {
        when(analyticsRepository.fetchBenchmarkProviderRules()).thenReturn(
                Collections.singletonList(rule(1, "MSCI.*", "MSCI", "INDEX")));
        benchmarkItemProcessor.setup(mock(StepExecution.class));

        BenchmarkMaster result = benchmarkItemProcessor.process(source("FTSE 100", "ACC12345"));

        assertNotNull(result);
        assertEquals("Other / Self-Indexed", result.getBenchmarkProvider());
        assertEquals("Category / Secondary", result.getBenchmarkType());
    }

    @Test
    void testProcess_RuleMatchingIsCaseInsensitive() {
        when(analyticsRepository.fetchBenchmarkProviderRules()).thenReturn(
                Collections.singletonList(rule(1, "s&p global", "S&P Global", "INDEX")));
        benchmarkItemProcessor.setup(mock(StepExecution.class));

        BenchmarkMaster result = benchmarkItemProcessor.process(source("S&P GLOBAL INDIA", "ACC12345"));

        assertNotNull(result);
        assertEquals("S&P Global", result.getBenchmarkProvider());
        assertEquals("INDEX", result.getBenchmarkType());
    }

    @Test
    void testProcess_IgnoresNonRegexRulesWithBlankPattern() {
        when(analyticsRepository.fetchBenchmarkProviderRules()).thenReturn(
                Collections.singletonList(rule(1, "   ", "Blank Pattern Provider", "BROKEN")));
        benchmarkItemProcessor.setup(mock(StepExecution.class));

        BenchmarkMaster result = benchmarkItemProcessor.process(source("S&P 500", "ACC12345"));

        assertNotNull(result);
        assertEquals("Other / Self-Indexed", result.getBenchmarkProvider());
        assertEquals("Category / Secondary", result.getBenchmarkType());
    }

    @Test
    void testSetup_FallsBackToDefaultBoilerplatePattern_WhenConfiguredRegexIsInvalid() {
        BenchmarkItemProcessor processor = buildProcessor("[invalid(");
        processor.setup(mock(StepExecution.class));

        BenchmarkMaster result = processor.process(
                source("S&P 500 (reflects no deduction for taxes)", "ACC12345"));

        assertNotNull(result);
        assertEquals("S&P 500", result.getBenchmarkName());
    }

    @Test
    void testSetup_SkipsRuleWithCorruptedRegex() {
        when(analyticsRepository.fetchBenchmarkProviderRules()).thenReturn(Arrays.asList(
                rule(1, "[unclosed", "Corrupt Provider", "BROKEN"),
                rule(2, "S&P.*", "S&P Global", "INDEX")));
        benchmarkItemProcessor.setup(mock(StepExecution.class));

        // Only the valid rule's pattern should have been compiled
        Map<?, ?> patternCache = (Map<?, ?>) ReflectionTestUtils.getField(benchmarkItemProcessor, "patternCache");
        assertNotNull(patternCache);
        assertEquals(1, patternCache.size());

        BenchmarkMaster result = benchmarkItemProcessor.process(source("S&P 500", "ACC12345"));
        assertNotNull(result);
        assertEquals("S&P Global", result.getBenchmarkProvider());
        assertEquals("INDEX", result.getBenchmarkType());
    }

    @Test
    void testSetup_ToleratesNullRulesFromRepository() {
        when(analyticsRepository.fetchBenchmarkProviderRules()).thenReturn(null);

        assertDoesNotThrow(() -> benchmarkItemProcessor.setup(mock(StepExecution.class)));
    }
}
