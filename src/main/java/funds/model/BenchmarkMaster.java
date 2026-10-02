package funds.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class BenchmarkMaster extends BaseModel {
    private UUID benchmarkId;
    private String benchmarkName;
    private String benchmarkType;
    private String benchmarkProvider;
    private String accessionNumber;

}
