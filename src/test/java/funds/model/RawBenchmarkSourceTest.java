package funds.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RawBenchmarkSourceTest {

    @Test
    void testNoArgsConstructor_DefaultsAllFieldsToNull() {
        RawBenchmarkSource source = new RawBenchmarkSource();

        assertNull(source.getAccessionNumber());
        assertNull(source.getSeriesId());
        assertNull(source.getRawMeasure());
        assertNull(source.getTerseLabel());
        assertNull(source.getStdLabel());
        assertNull(source.getVerboseLabel());
    }

    @Test
    void testAllArgsConstructor_SetsAllFieldsInDeclarationOrder() {
        RawBenchmarkSource source = new RawBenchmarkSource(
                "ACC12345",
                "S000001",
                "SPFiveHundredIndexMember",
                "S&P 500",
                "S&P 500 Index [Member]",
                "Standard and Poor 500 Index",
                "S&P 500 Index"
        );

        assertEquals("ACC12345", source.getAccessionNumber());
        assertEquals("S000001", source.getSeriesId());
        assertEquals("SPFiveHundredIndexMember", source.getRawMeasure());
        assertEquals("S&P 500", source.getTerseLabel());
        assertEquals("S&P 500 Index [Member]", source.getStdLabel());
        assertEquals("Standard and Poor 500 Index", source.getVerboseLabel());
        assertEquals("S&P 500 Index", source.getBenchmarkName());
    }

    @Test
    void testSettersAndGetters_RoundTrip() {
        RawBenchmarkSource source = new RawBenchmarkSource();

        source.setAccessionNumber("ACC67890");
        source.setSeriesId("S000002");
        source.setStdLabel("MSCI World Index");

        assertEquals("ACC67890", source.getAccessionNumber());
        assertEquals("S000002", source.getSeriesId());
        assertEquals("MSCI World Index", source.getStdLabel());
    }
}