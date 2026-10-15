package funds.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BenchmarkCandidateTest {

    @Test
    void testNoArgsConstructor_DefaultsAllFieldsToNull() {
        BenchmarkCandidate candidate = new BenchmarkCandidate();

        assertNull(candidate.getRuleId());
        assertNull(candidate.getPattern());
        assertNull(candidate.getProviderName());
        assertNull(candidate.getDefaultType());
        assertNull(candidate.getPriority());
        assertNull(candidate.getIsActive());
    }

    @Test
    void testAllArgsConstructor_SetsAllFieldsInDeclarationOrder() {
        BenchmarkCandidate candidate = new BenchmarkCandidate(1, "S&P 500", "Vanguard", "INDEX", 10, true);

        assertEquals(1, candidate.getRuleId());
        assertEquals("S&P 500", candidate.getPattern());
        assertEquals("Vanguard", candidate.getProviderName());
        assertEquals("INDEX", candidate.getDefaultType());
        assertEquals(10, candidate.getPriority());
        assertEquals(true, candidate.getIsActive());
    }

    @Test
    void testSettersAndGetters_RoundTrip() {
        BenchmarkCandidate candidate = new BenchmarkCandidate();

        candidate.setRuleId(2);
        candidate.setPattern("MSCI.*");
        candidate.setProviderName("MSCI");
        candidate.setDefaultType("CUSTOM");
        candidate.setPriority(5);
        candidate.setIsActive(false);

        assertEquals(2, candidate.getRuleId());
        assertEquals("MSCI.*", candidate.getPattern());
        assertEquals("MSCI", candidate.getProviderName());
        assertEquals("CUSTOM", candidate.getDefaultType());
        assertEquals(5, candidate.getPriority());
        assertEquals(false, candidate.getIsActive());
    }
}
