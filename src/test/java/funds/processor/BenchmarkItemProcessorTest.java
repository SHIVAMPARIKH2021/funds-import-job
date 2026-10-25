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

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BenchmarkItemProcessorTest {

    private static final String DEFAULT_BOILERPLATE_REGEX = "\\s*\\(reflects no deduction for [^)]*\\)";
    private static final String DEFAULT_NARRATIVE_REGEX =
            "(?:broad(?:-|\\s+)based\\s+(?:securities\\s+)?market\\s+index|broad\\s+measure\\s+of\\s+market\\s+performance|compare(?:d|s)?\\s+(?:with|to)\\s+(?:those\\s+of\\s+)?)(?:,\\s*the|\\s+the)?\\s+([A-Z0-9][A-Za-z0-9&/.,\\-\\s]{3,70}?)(?:\\s*\\(reflects|\\s*,?\\s*which\\s+reflects|\\s*\\.\\s*|\\s*,\\s*and\\b|\\s*\\band\\b|$)";
    private static final String DEFAULT_SUFFIX_REGEX = "(?i)(\\s*\\[Member\\]|Member)$";
    private static final String DEFAULT_TRANSITION_REGEX =
            "(?i)(?:index|benchmark)\\s+(?:has\\s+)?changed\\s+from\\s+.+?\\s+to\\s+(?:the\\s+)?(.+?)(?:\\s*,|\\s*\\(|\\s*\\.\\s*$)";

    @Mock
    private AnalyticsRepository analyticsRepository;

    private BenchmarkItemProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.processor = new BenchmarkItemProcessor(
                analyticsRepository,
                DEFAULT_BOILERPLATE_REGEX,
                DEFAULT_NARRATIVE_REGEX,
                DEFAULT_SUFFIX_REGEX,
                DEFAULT_TRANSITION_REGEX,
                "TEST_EXECUTOR"
        );
    }

    @Test
    void testProcess_SetsCreatedByAndTrimsAccessionNumber() throws Exception {
        RawBenchmarkSource source = new RawBenchmarkSource();
        source.setAccessionNumber("  0000005138-24-000022  ");
        source.setTerseLabel("S&P 500 Index (reflects no deduction for fees)");

        BenchmarkCandidate rule = new BenchmarkCandidate();
        rule.setRuleId(1);
        rule.setPattern("S&P 500");
        rule.setProviderName("Standard & Poor's");
        rule.setDefaultType("Broad-Based");
        when(analyticsRepository.fetchBenchmarkProviderRules()).thenReturn(List.of(rule));

        processor.setup(mock(StepExecution.class));

        BenchmarkMaster result = processor.process(source);

        assertNotNull(result);
        assertEquals("0000005138-24-000022", result.getAccessionNumber());
        assertEquals("TEST_EXECUTOR", result.getCreatedBy());
        assertEquals("TEST_EXECUTOR", result.getUpdatedBy());
        assertEquals("S&P 500 Index", result.getBenchmarkName());
        assertEquals("Standard & Poor's", result.getBenchmarkProvider());
    }

    @Test
    void testSetup_HandlesInvalidConfiguredRegexGracefully() {
        BenchmarkItemProcessor brokenRegexProcessor = new BenchmarkItemProcessor(
                analyticsRepository,
                "[invalid(regex",
                DEFAULT_NARRATIVE_REGEX,
                DEFAULT_SUFFIX_REGEX,
                DEFAULT_TRANSITION_REGEX,
                "TEST_EXECUTOR"
        );

        when(analyticsRepository.fetchBenchmarkProviderRules()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> brokenRegexProcessor.setup(mock(StepExecution.class)));

        Pattern boilerplatePattern = (Pattern) ReflectionTestUtils.getField(brokenRegexProcessor, "boilerplatePattern");
        assertNull(boilerplatePattern);
    }

    @Test
    void testExtractTransitionMatchFromNarrative_ExtractsNewTargetIndex() throws Exception {
        RawBenchmarkSource source = new RawBenchmarkSource();
        source.setAccessionNumber("0001398344-24-006650");
        source.setRawMeasure(null);

        RawBenchmarkSource narrativeSource = new RawBenchmarkSource();
        narrativeSource.setRawMeasure(
                "Effective May 1, 2024, pursuant to new regulatory requirements, the Fund's broad-based securities market index has changed from the ICE BofA 3-month U.S. Treasury Bill Index to the Bloomberg U.S. Aggregate Bond Index."
        );
        narrativeSource.setStdLabel("PerformanceNarrativeTextBlock");

        when(analyticsRepository.findUnresolvedBenchmarksFromTextFacts(anyList(), eq("0001398344-24-006650")))
                .thenReturn(List.of(narrativeSource));

        BenchmarkCandidate rule = new BenchmarkCandidate();
        rule.setRuleId(2);
        rule.setPattern("Bloomberg");
        rule.setProviderName("Bloomberg");
        rule.setDefaultType("Broad-Based");
        when(analyticsRepository.fetchBenchmarkProviderRules()).thenReturn(List.of(rule));

        processor.setup(mock(StepExecution.class));

        BenchmarkMaster result = processor.process(source);

        assertNotNull(result);
        assertEquals("Bloomberg U.S. Aggregate Bond Index", result.getBenchmarkName());
        assertEquals("Bloomberg", result.getBenchmarkProvider());
    }
}