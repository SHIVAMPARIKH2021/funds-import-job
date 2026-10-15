package funds.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FundMasterTest {

    @Test
    void testNoArgsConstructor_DefaultsCadenceAndFlags() {
        FundMaster fund = new FundMaster();

        assertEquals("ANNUAL", fund.getReportingCadence());
        assertEquals(true, fund.getHasDailyPricing());
        assertEquals(true, fund.getHasQuarterlyHoldings());
        assertNull(fund.getFundId());
        assertNull(fund.getSeriesId());
        assertNull(fund.getCik());
        assertNull(fund.getFundName());
        assertNull(fund.getFundFamily());
        assertNull(fund.getInvestmentObjective());
        assertNull(fund.getStrategyNarrative());
        assertNull(fund.getStrategyType());
        assertNull(fund.getBenchmarkId());
        assertNull(fund.getBenchmarkName());
        assertNull(fund.getPrimaryTicker());
        assertNull(fund.getAccessionNumber());
    }

    @Test
    void testAllArgsConstructor_SetsAllFieldsInDeclarationOrder() {
        FundMaster fund = new FundMaster(
                1, "TEST123", 1234567890, "Test Fund", "Test Family",
                "Growth", "Strategy narrative", "Active", 101, "S&P 500 Index",
                "TEST", "ACC12345", "QUARTERLY", false, false);

        assertEquals(1, fund.getFundId());
        assertEquals("TEST123", fund.getSeriesId());
        assertEquals(1234567890, fund.getCik());
        assertEquals("Test Fund", fund.getFundName());
        assertEquals("Test Family", fund.getFundFamily());
        assertEquals("Growth", fund.getInvestmentObjective());
        assertEquals("Strategy narrative", fund.getStrategyNarrative());
        assertEquals("Active", fund.getStrategyType());
        assertEquals(101, fund.getBenchmarkId());
        assertEquals("S&P 500 Index", fund.getBenchmarkName());
        assertEquals("TEST", fund.getPrimaryTicker());
        assertEquals("ACC12345", fund.getAccessionNumber());
        assertEquals("QUARTERLY", fund.getReportingCadence());
        assertEquals(false, fund.getHasDailyPricing());
        assertEquals(false, fund.getHasQuarterlyHoldings());
    }

    @Test
    void testSettersAndGetters_RoundTrip() {
        FundMaster fund = new FundMaster();

        fund.setFundId(1);
        fund.setSeriesId("TEST123");
        fund.setCik(1234567890);
        fund.setFundName("Test Fund");
        fund.setFundFamily("Test Family");
        fund.setInvestmentObjective("Growth and capital appreciation");
        fund.setStrategyNarrative("Our fund focuses on technology");
        fund.setStrategyType("Active");
        fund.setBenchmarkId(101);
        fund.setBenchmarkName("S&P 500 Index");
        fund.setPrimaryTicker("TEST");
        fund.setAccessionNumber("ACC12345");
        fund.setReportingCadence("ANNUAL");
        fund.setHasDailyPricing(true);
        fund.setHasQuarterlyHoldings(true);

        assertEquals(1, fund.getFundId());
        assertEquals("TEST123", fund.getSeriesId());
        assertEquals(1234567890, fund.getCik());
        assertEquals("Test Fund", fund.getFundName());
        assertEquals("Test Family", fund.getFundFamily());
        assertEquals("Growth and capital appreciation", fund.getInvestmentObjective());
        assertEquals("Our fund focuses on technology", fund.getStrategyNarrative());
        assertEquals("Active", fund.getStrategyType());
        assertEquals(101, fund.getBenchmarkId());
        assertEquals("S&P 500 Index", fund.getBenchmarkName());
        assertEquals("TEST", fund.getPrimaryTicker());
        assertEquals("ACC12345", fund.getAccessionNumber());
        assertEquals("ANNUAL", fund.getReportingCadence());
        assertEquals(true, fund.getHasDailyPricing());
        assertEquals(true, fund.getHasQuarterlyHoldings());
    }
}
