package funds.model;

import lombok.Data;

@Data
public class ComplianceRules extends BaseModel {
    private Boolean isActive;
    private Boolean isRegex;
    private String pattern;
    private String ruleName;
    private Integer ruleId;
    private Integer priority;
    private String targetStrategy;
}
