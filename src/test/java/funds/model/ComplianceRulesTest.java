package funds.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ComplianceRulesTest {

    @Test
    void testNoArgsConstructor_DefaultsAllFieldsToNull() {
        ComplianceRules rule = new ComplianceRules();

        assertNull(rule.getIsActive());
        assertNull(rule.getIsRegex());
        assertNull(rule.getPattern());
        assertNull(rule.getRuleName());
        assertNull(rule.getRuleId());
        assertNull(rule.getPriority());
        assertNull(rule.getTargetStrategy());
    }

    @Test
    void testAllArgsConstructor_SetsAllFieldsInDeclarationOrder() {
        ComplianceRules rule = new ComplianceRules(
                true, true, "match.*rule", "ruleName", 1, 1, "TARGET_STRATEGY");

        assertEquals(true, rule.getIsActive());
        assertEquals(true, rule.getIsRegex());
        assertEquals("match.*rule", rule.getPattern());
        assertEquals("ruleName", rule.getRuleName());
        assertEquals(1, rule.getRuleId());
        assertEquals(1, rule.getPriority());
        assertEquals("TARGET_STRATEGY", rule.getTargetStrategy());
    }

    @Test
    void testSettersAndGetters_RoundTrip() {
        ComplianceRules rule = new ComplianceRules();

        rule.setIsActive(false);
        rule.setIsRegex(false);
        rule.setPattern("literal text");
        rule.setRuleName("Literal Rule");
        rule.setRuleId(7);
        rule.setPriority(3);
        rule.setTargetStrategy("Passive");

        assertEquals(false, rule.getIsActive());
        assertEquals(false, rule.getIsRegex());
        assertEquals("literal text", rule.getPattern());
        assertEquals("Literal Rule", rule.getRuleName());
        assertEquals(7, rule.getRuleId());
        assertEquals(3, rule.getPriority());
        assertEquals("Passive", rule.getTargetStrategy());
    }
}
