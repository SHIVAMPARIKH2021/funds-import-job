package funds.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "analytics", name = "compliance_rules")
@Getter
@Setter
@NoArgsConstructor
public class ComplianceRules extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer ruleId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "pattern", nullable = false, columnDefinition = "TEXT")
    private String pattern;

    @Column(name = "is_regex", nullable = false)
    private Boolean isRegex;

    @Column(name = "target_strategy", nullable = false, length = 20)
    private String targetStrategy;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy = "SYSTEM";
}