package funds.processor;

import funds.model.BenchmarkCandidate;
import funds.model.BenchmarkMaster;
import funds.model.RawBenchmarkSource;
import funds.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    private static final String DEFAULT_BOILERPLATE_REGEX = "\\s*\\((?:(?:(?:Index|the)\\s+)?(?:returns?\\s+)?(?:reflects?|do(?:es)?\\s+not\\s+reflect)|no\\s+deductions?)\\b[^)]*\\)";
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

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @DisplayName("Verify fee/tax boilerplate clauses are stripped while preserving index names and qualifiers")
    @CsvSource(delimiter = '|', value = {
            // === 1. Must Strip: Variations from 202610262321.csv ===
            "Bloomberg US Aggregate Bond Index (reflects no deductions for fees, expenses, or taxes) | Bloomberg US Aggregate Bond Index",
            "S&P 500 Index (reflects no deduction for fees, expenses, or taxes)                       | S&P 500 Index",
            "Russell 2000 Index (Does not reflect fees, expenses or taxes, which would reduce)       | Russell 2000 Index",
            "Putnam Equity Blended Index (no deduction for fees, expenses or taxes)                  | Putnam Equity Blended Index",
            "Balanced Composite Index (Does not reflect fees, expenses or taxes)                     | Balanced Composite Index",
            "HFRI Fund of Funds Index (Does not reflect fees, expenses or taxes)                     | HFRI Fund of Funds Index",
            "S&P 500 Low Volatility Index (reflects reinvestment of dividends)                       | S&P 500 Low Volatility Index",
            "Bond Index (returns reflect no deduction for fees)                                      | Bond Index",
            "Equity Index (Index reflects no deductions for sales charges)                           | Equity Index",
            "Global X Defense Tech Index (NR) (USD) (Index returns do not reflect fees)              | Global X Defense Tech Index (NR) (USD)",
            "MSCI All Country World Index (Net) (reflects reinvested dividends net of withholding)   | MSCI All Country World Index (Net)",
            "Custom Index (reflects withholding taxes on foreign dividends)                          | Custom Index",
            "Market Index (reflects deductions for fees and expenses)                                | Market Index",

            // === 2. Must Preserve: Legitimate Parenthetical Qualifiers ===
            "MSCI EAFE Index (Net)                                                                   | MSCI EAFE Index (Net)",
            "MSCI ACWI ex USA Index (ND)                                                             | MSCI ACWI ex USA Index (ND)",
            "Global X Defense Tech Index (NR) (USD)                                                  | Global X Defense Tech Index (NR) (USD)",
            "Bloomberg Municipal Bond 5 Year Total Return Index (BM5I)                               | Bloomberg Municipal Bond 5 Year Total Return Index (BM5I)",
            "S&P 500 Index (PR)                                                                      | S&P 500 Index (PR)",
            "Russell 2000 Value Index (TR)                                                           | Russell 2000 Value Index (TR)",
            "Barclays Capital TIPS Index (TIPS)                                                      | Barclays Capital TIPS Index (TIPS)",
            "MSCI World Index (Gross)                                                                | MSCI World Index (Gross)",

            // === 3. Must Preserve: Composite / Blended Benchmarks ===
            "60% MSCI World Index (Hedged to USD) / 40% Bloomberg Global-Aggregate                   | 60% MSCI World Index (Hedged to USD) / 40% Bloomberg Global-Aggregate",
            "Hybrid Index (60% MSCI World Index and 40% FTSE World Government Bond Index)            | Hybrid Index (60% MSCI World Index and 40% FTSE World Government Bond Index)",
            "Reference Benchmark (an unmanaged weighted index consisting of 65% S&P 500)             | Reference Benchmark (an unmanaged weighted index consisting of 65% S&P 500)"
    })
    void testBoilerplateRegexAccuracy(String rawInput, String expectedOutput) {
        String cleaned = Pattern.compile(DEFAULT_BOILERPLATE_REGEX, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                .matcher(rawInput).replaceAll("").trim();
        assertEquals(expectedOutput, cleaned);
    }
}