package funds.processor;

import funds.constants.Tags;
import funds.model.BenchmarkMaster;
import funds.model.ComplianceRules;
import funds.model.FundBenchmarkAssociation;
import funds.model.FundImportPayload;
import funds.model.FundMaster;
import funds.model.RawFundSource;
import funds.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepExecution;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FundsItemProcessorTest {

    private static final int DEFAULT_BENCHMARK_ID = 0;
    private static final String DEFAULT_BENCHMARK_NAME = "Not Disclosed / New Inception";

    @Mock
    private AnalyticsRepository analyticsRepository;

    private FundsItemProcessor fundsItemProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.fundsItemProcessor = new FundsItemProcessor(
                analyticsRepository,
                "TEST",
                "false",
                DEFAULT_BENCHMARK_ID
        );
    }

    @Test
    void testProcess_NullItemReturnsNull() throws Exception {
        FundImportPayload result = fundsItemProcessor.process(null);
        assertNull(result);
    }

    @Test
    void testProcess_SkipsItemWithNullSeriesId() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId(null);
        FundImportPayload result = fundsItemProcessor.process(item);
        assertNull(result);
    }

    @Test
    void testProcess_SkipsItemWithEmptySeriesId() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId("");
        FundImportPayload result = fundsItemProcessor.process(item);
        assertNull(result);
    }

    @Test
    void testProcess_SkipsDryRun() throws Exception {
        FundsItemProcessor dryRunProcessor = new FundsItemProcessor(
                analyticsRepository,
                "TEST",
                "true",
                DEFAULT_BENCHMARK_ID
        );

        RawFundSource item = new RawFundSource();
        item.setSeriesId("TEST");
        item.setDisclosureValue("Test strategy");

        FundImportPayload result = dryRunProcessor.process(item);
        assertNull(result);
    }

    @Test
    void testProcess_WithComplianceRuleMatch() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId("TEST123");
        item.setAccessionNumber("ACC12345");
        item.setDisclosureValue("This fund has a value that matches the compliance rule");
        item.setDisclosureTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());

        List<ComplianceRules> rules = Collections.singletonList(new ComplianceRules(
                true, true, "match.*rule", "ruleName", 1, 1, "TARGET_STRATEGY"));
        when(analyticsRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(rules);

        Map<String, String> fundNames = Collections.singletonMap("TEST123", "Test Fund");
        when(analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag())).thenReturn(fundNames);
        when(analyticsRepository.mapfindBenchmarksGroupedByAccession()).thenReturn(Collections.emptyMap());

        fundsItemProcessor.setup(mock(StepExecution.class));

        FundImportPayload result = fundsItemProcessor.process(item);
        assertNotNull(result);
        assertEquals("TEST123", result.getFundMaster().getSeriesId());
        assertEquals("TARGET_STRATEGY", result.getFundMaster().getStrategyType());
        assertEquals("Test Fund", result.getFundMaster().getFundName());
        assertEquals(DEFAULT_BENCHMARK_ID, result.getFundMaster().getBenchmarkId());
        assertEquals(DEFAULT_BENCHMARK_NAME, result.getFundMaster().getBenchmarkName());
        assertEquals(1, result.getAssociations().size());
        assertEquals(DEFAULT_BENCHMARK_ID, result.getAssociations().get(0).getBenchmarkId());
    }

    @Test
    void testProcess_NoComplianceRuleMatchReturnsUnknown() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId("TEST123");
        item.setAccessionNumber("ACC12345");
        item.setDisclosureValue("Unknown strategy value here");
        item.setDisclosureTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());

        List<ComplianceRules> rules = Collections.singletonList(new ComplianceRules(
                true, true, "other.*pattern", "ruleName", 1, 1, "OTHER_STRATEGY"));
        when(analyticsRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(rules);
        when(analyticsRepository.mapfindBenchmarksGroupedByAccession()).thenReturn(Collections.emptyMap());
        when(analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag()))
                .thenReturn(Collections.singletonMap("TEST123", "Test Fund"));

        fundsItemProcessor.setup(mock(StepExecution.class));

        FundImportPayload result = fundsItemProcessor.process(item);
        assertNotNull(result);
        assertEquals("UNKNOWN", result.getFundMaster().getStrategyType());
    }

    @Test
    void testProcess_WithStrategyNarrative() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId("TEST123");
        item.setAccessionNumber("ACC12345");
        item.setDisclosureValue("Our fund focuses on technology and innovation");
        item.setDisclosureTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());

        when(analyticsRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(analyticsRepository.mapfindBenchmarksGroupedByAccession()).thenReturn(Collections.emptyMap());
        when(analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag()))
                .thenReturn(Collections.singletonMap("TEST123", "Test Fund"));

        fundsItemProcessor.setup(mock(StepExecution.class));

        FundImportPayload result = fundsItemProcessor.process(item);
        assertNotNull(result);
        assertNotNull(result.getFundMaster().getStrategyNarrative());
        assertEquals("Our fund focuses on technology and innovation", result.getFundMaster().getStrategyNarrative());
    }

    @Test
    void testProcess_WithInvestmentObjective() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId("TEST123");
        item.setAccessionNumber("ACC12345");
        item.setDisclosureValue("Growth and capital appreciation");
        item.setDisclosureTag(Tags.OBJECTIVE_PRIMARY_TEXT_BLOCK.getTag());

        when(analyticsRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(analyticsRepository.mapfindBenchmarksGroupedByAccession()).thenReturn(Collections.emptyMap());
        when(analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag()))
                .thenReturn(Collections.singletonMap("TEST123", "Test Fund"));

        fundsItemProcessor.setup(mock(StepExecution.class));

        FundImportPayload result = fundsItemProcessor.process(item);
        assertNotNull(result);
        assertNotNull(result.getFundMaster().getInvestmentObjective());
        assertEquals("Growth and capital appreciation", result.getFundMaster().getInvestmentObjective());
    }

    @Test
    void testProcess_WithBenchmarkAssocations() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId("TEST123");
        item.setAccessionNumber("ACC12345");
        item.setDisclosureValue("Strategy narrative");
        item.setDisclosureTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());

        when(analyticsRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag()))
                .thenReturn(Collections.emptyMap());

        Map<String, List<BenchmarkMaster>> benchmarksMap = new HashMap<>();
        BenchmarkMaster bm = new BenchmarkMaster();
        bm.setBenchmarkId(1);
        bm.setBenchmarkName("Benchmark1");
        bm.setBenchmarkType("Type1");
        benchmarksMap.put("ACC12345", Collections.singletonList(bm));
        when(analyticsRepository.mapfindBenchmarksGroupedByAccession()).thenReturn(benchmarksMap);

        fundsItemProcessor.setup(mock(StepExecution.class));

        FundImportPayload result = fundsItemProcessor.process(item);
        assertNotNull(result);
        assertNotNull(result.getAssociations());
        assertEquals(1, result.getAssociations().size());
        assertEquals("TEST123", result.getAssociations().get(0).getSeriesId());
        assertEquals(1, result.getAssociations().get(0).getBenchmarkId());
        assertEquals("Benchmark1", result.getFundMaster().getBenchmarkName());
        assertTrue(result.getAssociations().get(0).getIsPrimary());
    }

    @Test
    void testProcess_WithFundNamesByTagMapping() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId("TEST123");
        item.setAccessionNumber("ACC12345");
        item.setDisclosureValue("Strategy narrative");
        item.setDisclosureTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());

        when(analyticsRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(analyticsRepository.mapfindBenchmarksGroupedByAccession()).thenReturn(Collections.emptyMap());
        when(analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag()))
                .thenReturn(Collections.singletonMap("TEST123", "Mapped Fund Name"));

        fundsItemProcessor.setup(mock(StepExecution.class));

        FundImportPayload result = fundsItemProcessor.process(item);
        assertNotNull(result);
        assertEquals("Mapped Fund Name", result.getFundMaster().getFundName());
    }

    @Test
    void testProcess_WithMultipleBenchmarkAssociations() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId("TEST123");
        item.setAccessionNumber("ACC12345");
        item.setDisclosureValue("Strategy narrative");
        item.setDisclosureTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());

        when(analyticsRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag()))
                .thenReturn(Collections.emptyMap());

        Map<String, List<BenchmarkMaster>> benchmarksMap = new HashMap<>();
        BenchmarkMaster bm1 = new BenchmarkMaster();
        bm1.setBenchmarkId(1);
        bm1.setBenchmarkName("Benchmark1");
        BenchmarkMaster bm2 = new BenchmarkMaster();
        bm2.setBenchmarkId(2);
        bm2.setBenchmarkName("Benchmark2");
        benchmarksMap.put("ACC12345", Arrays.asList(bm1, bm2));
        when(analyticsRepository.mapfindBenchmarksGroupedByAccession()).thenReturn(benchmarksMap);

        fundsItemProcessor.setup(mock(StepExecution.class));

        FundImportPayload result = fundsItemProcessor.process(item);
        assertNotNull(result);
        assertEquals(2, result.getAssociations().size());
        assertEquals(true, result.getAssociations().get(0).getIsPrimary());
        assertEquals(false, result.getAssociations().get(1).getIsPrimary());
        assertEquals(1, result.getFundMaster().getBenchmarkId());
        assertEquals("Benchmark1", result.getFundMaster().getBenchmarkName());
    }

    @Test
    void testProcess_NoBenchmarkDisclosed_AppliesDomainFallback() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId("NEW_INCEPTION_123");
        item.setAccessionNumber("ACC_NO_BENCHMARK");
        item.setDisclosureValue("Strategy narrative");
        item.setDisclosureTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());

        when(analyticsRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(analyticsRepository.mapfindBenchmarksGroupedByAccession()).thenReturn(Collections.emptyMap());
        when(analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag()))
                .thenReturn(Collections.emptyMap());

        fundsItemProcessor.setup(mock(StepExecution.class));

        FundImportPayload result = fundsItemProcessor.process(item);
        assertNotNull(result);

        FundMaster fund = result.getFundMaster();
        assertEquals(DEFAULT_BENCHMARK_ID, fund.getBenchmarkId());
        assertEquals(DEFAULT_BENCHMARK_NAME, fund.getBenchmarkName());

        assertNotNull(result.getAssociations());
        assertEquals(1, result.getAssociations().size());
        FundBenchmarkAssociation association = result.getAssociations().get(0);
        assertEquals("NEW_INCEPTION_123", association.getSeriesId());
        assertEquals(DEFAULT_BENCHMARK_ID, association.getBenchmarkId());
        assertEquals(DEFAULT_BENCHMARK_NAME, association.getBenchmarkName());
        assertTrue(association.getIsPrimary());
    }

    @Test
    void testProcess_CreateBaseFundEntityWithDefaults() throws Exception {
        RawFundSource item = new RawFundSource();
        item.setSeriesId("TEST123");
        item.setAccessionNumber("ACC12345");
        item.setDisclosureValue("Strategy narrative");
        item.setDisclosureTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag());
        item.setTicker("TEST");
        item.setCik(1234567890);
        item.setFundFamily("Test Family");

        when(analyticsRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(Collections.emptyList());
        when(analyticsRepository.mapfindBenchmarksGroupedByAccession()).thenReturn(Collections.emptyMap());
        when(analyticsRepository.findFundNamesByTag(Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag()))
                .thenReturn(Collections.singletonMap("TEST123", "Test Fund"));

        fundsItemProcessor.setup(mock(StepExecution.class));

        FundImportPayload result = fundsItemProcessor.process(item);
        assertNotNull(result);
        FundMaster fund = result.getFundMaster();
        assertEquals("TEST123", fund.getSeriesId());
        assertEquals("Test Fund", fund.getFundName());
        assertEquals("Test Family", fund.getFundFamily());
        assertEquals("TEST", fund.getPrimaryTicker());
        assertEquals("ANNUAL", fund.getReportingCadence());
        assertEquals(true, fund.getHasDailyPricing());
        assertEquals(true, fund.getHasQuarterlyHoldings());
        assertNotNull(fund.getCreatedBy());
    }
}