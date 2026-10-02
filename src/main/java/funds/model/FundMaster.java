package funds.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FundMaster extends BaseModel {
    private Integer fundId;
    private String seriesId;
    private Integer cik;
    private String fundName;
    private String fundFamily;
    private String investmentObjective;
    private String strategyNarrative;
    private String strategyType;
    private UUID benchmarkId;
    private String benchmarkName;
    private String primaryTicker;
    private String accessionNumber;
    // New cadence & update flags
    /** Because we are using StrategyNarrativeTextBlock, hence the default cadence is annual.
    This can be updated later if we find a fund with a different cadence.
    **/
    private String reportingCadence = "ANNUAL";
    private Boolean hasDailyPricing = Boolean.TRUE;
    private Boolean hasQuarterlyHoldings = Boolean.TRUE;
}
