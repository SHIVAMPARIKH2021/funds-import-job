package funds.constants;

public enum Tags {

    AVERAGE_ANNUAL_RETURN_ONE_YEAR("AverageAnnualReturnYear01"),
    AVERAGE_ANNUAL_RETURN_THREE_YEAR("AverageAnnualReturnYear03"),
    AVERAGE_ANNUAL_RETURN_FIVE_YEAR("AverageAnnualReturnYear05"),
    AVERAGE_ANNUAL_RETURN_TEN_YEAR("AverageAnnualReturnYear10"),
    AVERAGE_ANNUAL_RETURN_FIFTEEN_YEAR("AverageAnnualReturnYear15"),
    AVERAGE_ANNUAL_RETURN_TWENTY_FIVE_YEAR("AverageAnnualReturnYear25"),
    AVERAGE_ANNUAL_RETURN_INCEPTION("AverageAnnualReturnSinceInception"),
    AVERAGE_ANNUAL_RETURN_INCEPTION_ONE("AverageAnnualReturnSinceInception1"),
    AVERAGE_ANNUAL_RETURN_INCEPTION_TWO("AverageAnnualReturnSinceInception2"),
    RETURN_BEFORE_TAXES("ReturnBeforeTaxes"),
    AFTER_TAXES_ON_DISTRIBUTIONS("AfterTaxesOnDistributions"),
    AFTER_TAXES_ON_DISTRIBUTIONS_AND_SALES("AfterTaxesOnDistributionsAndSales"),
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
