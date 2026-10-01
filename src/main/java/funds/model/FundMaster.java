package funds.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
    private String benchmarkId;
    private String benchmarkName;
    private String primaryTicker;
}
