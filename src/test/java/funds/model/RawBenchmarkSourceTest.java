package funds.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RawBenchmarkSourceTest {

    @Test
    void testNoArgsConstructor_DefaultsAllFieldsToNull() {
        RawBenchmarkSource source = new RawBenchmarkSource();

        assertNull(source.getBenchmarkName());
        assertNull(source.getAccessionNumber());
    }

    @Test
    void testAllArgsConstructor_SetsAllFieldsInDeclarationOrder() {
        RawBenchmarkSource source = new RawBenchmarkSource("S&P 500 Index", "ACC12345");

        assertEquals("S&P 500 Index", source.getBenchmarkName());
        assertEquals("ACC12345", source.getAccessionNumber());
    }

    @Test
    void testSettersAndGetters_RoundTrip() {
        RawBenchmarkSource source = new RawBenchmarkSource();

        source.setBenchmarkName("MSCI World Index");
        source.setAccessionNumber("ACC67890");

        assertEquals("MSCI World Index", source.getBenchmarkName());
        assertEquals("ACC67890", source.getAccessionNumber());
    }
}
