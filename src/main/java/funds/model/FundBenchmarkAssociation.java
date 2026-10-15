package funds.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FundBenchmarkAssociation extends BaseModel {

    private Integer id;
    private String seriesId;
    private Integer benchmarkId;
    private String benchmarkName;
    private Boolean isPrimary = Boolean.FALSE;
    private String accessionNumber;

    public FundBenchmarkAssociation(String seriesId, Integer benchmarkId, String benchmarkName, Boolean isPrimary, String accessionNumber) {
        this.seriesId = seriesId;
        this.benchmarkId = benchmarkId;
        this.benchmarkName = benchmarkName;
        this.isPrimary = isPrimary;
        this.accessionNumber = accessionNumber;
    }
}