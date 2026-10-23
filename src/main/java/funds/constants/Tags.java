package funds.constants;

public enum Tags {

    // --- Performance Return Numeric Fact Tags ---
    AVERAGE_ANNUAL_RETURN_ONE_YEAR("AverageAnnualReturnYear01"),
    AVERAGE_ANNUAL_RETURN_THREE_YEAR("AverageAnnualReturnYear03"),
    AVERAGE_ANNUAL_RETURN_FIVE_YEAR("AverageAnnualReturnYear05"),
    AVERAGE_ANNUAL_RETURN_TEN_YEAR("AverageAnnualReturnYear10"),
    AVERAGE_ANNUAL_RETURN_FIFTEEN_YEAR("AverageAnnualReturnYear15"),
    AVERAGE_ANNUAL_RETURN_TWENTY_FIVE_YEAR("AverageAnnualReturnYear25"),
    AVERAGE_ANNUAL_RETURN_INCEPTION("AverageAnnualReturnSinceInception"),
    AVERAGE_ANNUAL_RETURN_INCEPTION_ONE("AverageAnnualReturnSinceInception1"),
    AVERAGE_ANNUAL_RETURN_INCEPTION_TWO("AverageAnnualReturnSinceInception2"),
    // Common alternative taxonomy tags for 1, 5, 10-year periods
    ANNUAL_RETURN_YEAR_01("AnnualReturnYear01"),
    ANNUAL_RETURN_YEAR_05("AnnualReturnYear05"),
    ANNUAL_RETURN_YEAR_10("AnnualReturnYear10"),

    // --- Non-Benchmark Performance Measure Members (EXCLUDED) ---
    RETURN_BEFORE_TAXES("ReturnBeforeTaxes"),
    RETURN_BEFORE_TAXES_MEMBER("ReturnBeforeTaxesMember"),
    AFTER_TAXES_ON_DISTRIBUTIONS("AfterTaxesOnDistributions"),
    AFTER_TAXES_ON_DISTRIBUTIONS_MEMBER("AfterTaxesOnDistributionsMember"),
    AFTER_TAXES_ON_DISTRIBUTIONS_AND_SALES("AfterTaxesOnDistributionsAndSales"),
    AFTER_TAXES_ON_DISTRIBUTIONS_AND_SALES_MEMBER("AfterTaxesOnDistributionsAndSalesMember"),
    // SEC lower-case/unspaced variation members
    AFTER_TAX_ON_DISTRIBUTIONS_LOWER("Aftertaxondistributions"),
    AFTER_TAX_ON_DISTRIBUTIONS_LOWER_MEMBER("AftertaxondistributionsMember"),
    AFTER_TAX_ON_DISTRIBUTIONS_AND_SALES_LOWER("Aftertaxondistributionsandsaleoffundshares"),
    AFTER_TAX_ON_DISTRIBUTIONS_AND_SALES_LOWER_MEMBER("AftertaxondistributionsandsaleoffundsharesMember"),

    // --- Text Fact / Narrative Tags (Item 4 Benchmarks & Disclosures) ---
    MARKET_INDEX_PERFORMANCE_COLUMN_HEADING("MarketIndexPerformanceColumnHeading"),
    INDEX_NO_DEDUCTION_FOR_FEES_EXPENSES_TAXES("IndexNoDeductionForFeesExpensesTaxes"),
    PERFORMANCE_TABLE_NARRATIVE("PerformanceTableNarrative"),
    STRATEGY_NARRATIVE_TEXT_BLOCK("StrategyNarrativeTextBlock"),
    OBJECTIVE_PRIMARY_TEXT_BLOCK("ObjectivePrimaryTextBlock"),
    OBJECTIVE_SECONDARY_TEXT_BLOCK("ObjectiveSecondaryTextBlock"),
    INVESTMENT_STRATEGY_TEXT_BLOCK("InvestmentStrategyTextBlock");

    private final String tag;

    Tags(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }
}