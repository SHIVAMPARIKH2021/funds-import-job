package funds.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComplianceRules extends BaseModel {
    private Boolean isActive;
    private Boolean isRegex;
    private String pattern;
    private String ruleName;
    private Integer ruleId;
    private Integer priority;
    private String targetStrategy;
}
