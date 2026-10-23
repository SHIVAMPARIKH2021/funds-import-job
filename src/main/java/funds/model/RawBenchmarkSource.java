package funds.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class RawBenchmarkSource extends BaseModel{
    private String accessionNumber;
    private String seriesId;
    private String rawMeasure;
    private String terseLabel;
    private String stdLabel;
    private String verboseLabel;
}
