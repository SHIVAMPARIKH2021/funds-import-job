package funds.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RawFundSourceTest {

    @Test
    void testNoArgsConstructor_DefaultsAllFieldsToNull() {
        RawFundSource source = new RawFundSource();

        assertNull(source.getSeriesId());
        assertNull(source.getCik());
        assertNull(source.getAccessionNumber());
        assertNull(source.getFilingDate());
        assertNull(source.getTicker());
        assertNull(source.getClassId());
        assertNull(source.getFundFamily());
        assertNull(source.getDisclosureValue());
        assertNull(source.getDisclosureTag());
        assertNull(source.getAdditonalStrategyNarrative());
        assertNull(source.getBenchmarkName());
        assertNull(source.getFilingQuarter());
        assertNull(source.getFilingYear());
        assertNull(source.getFormType());
    }

    @Test
    void testAllArgsConstructor_SetsAllFieldsInDeclarationOrder() {
        RawFundSource source = new RawFundSource(
                "TEST123", 1234567890, "ACC12345", LocalDate.of(2025, 1, 15),
                "485APOS","TEST", "CLASS_A", "Test Family",
                "Strategy narrative value", "STRATEGY_NARRATIVE_TEXT_BLOCK",
                "Additional narrative", "S&P 500 Index", "Q1", 2025);

        assertEquals("TEST123", source.getSeriesId());
        assertEquals(1234567890, source.getCik());
        assertEquals("ACC12345", source.getAccessionNumber());
        assertEquals(LocalDate.of(2025, 1, 15), source.getFilingDate());
        assertEquals("TEST", source.getTicker());
        assertEquals("CLASS_A", source.getClassId());
        assertEquals("Test Family", source.getFundFamily());
        assertEquals("Strategy narrative value", source.getDisclosureValue());
        assertEquals("STRATEGY_NARRATIVE_TEXT_BLOCK", source.getDisclosureTag());
        assertEquals("Additional narrative", source.getAdditonalStrategyNarrative());
        assertEquals("S&P 500 Index", source.getBenchmarkName());
        assertEquals("Q1", source.getFilingQuarter());
        assertEquals(2025, source.getFilingYear());
    }

    @Test
    void testSettersAndGetters_RoundTrip() {
        RawFundSource source = new RawFundSource();

        source.setSeriesId("TEST123");
        source.setCik(1234567890);
        source.setAccessionNumber("ACC12345");
        source.setFilingDate(LocalDate.of(2025, 1, 15));
        source.setTicker("TEST");
        source.setClassId("CLASS_A");
        source.setFundFamily("Test Family");
        source.setDisclosureValue("Strategy narrative value");
        source.setDisclosureTag("STRATEGY_NARRATIVE_TEXT_BLOCK");
        source.setAdditonalStrategyNarrative("Additional narrative");
        source.setBenchmarkName("S&P 500 Index");
        source.setFilingQuarter("Q1");
        source.setFilingYear(2025);

        assertEquals("TEST123", source.getSeriesId());
        assertEquals(1234567890, source.getCik());
        assertEquals("ACC12345", source.getAccessionNumber());
        assertEquals(LocalDate.of(2025, 1, 15), source.getFilingDate());
        assertEquals("TEST", source.getTicker());
        assertEquals("CLASS_A", source.getClassId());
        assertEquals("Test Family", source.getFundFamily());
        assertEquals("Strategy narrative value", source.getDisclosureValue());
        assertEquals("STRATEGY_NARRATIVE_TEXT_BLOCK", source.getDisclosureTag());
        assertEquals("Additional narrative", source.getAdditonalStrategyNarrative());
        assertEquals("S&P 500 Index", source.getBenchmarkName());
        assertEquals("Q1", source.getFilingQuarter());
        assertEquals(2025, source.getFilingYear());
    }

    @Test
    void testInheritedBaseModelFields_AreAccessible() {
        RawFundSource source = new RawFundSource();

        source.setCreatedBy("TEST");

        assertEquals("TEST", source.getCreatedBy());
    }
}
