package funds.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkCandidate extends BaseModel{
    private Integer ruleId;
    private String pattern;
    private String providerName;
    private String defaultType;
    private Integer priority;
    private Boolean isActive;
}
