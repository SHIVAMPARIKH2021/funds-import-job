package funds.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BenchmarkMaster extends BaseModel {
    private Integer benchmarkId;
    private String benchmarkName;
    private String benchmarkType;
    private String benchmarkProvider;
    private String accessionNumber;

}
