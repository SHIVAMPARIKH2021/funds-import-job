package funds.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BenchmarkMasterTest {

    @Test
    void testNoArgsConstructor_DefaultsAllFieldsToNull() {
        BenchmarkMaster benchmark = new BenchmarkMaster();

        assertNull(benchmark.getBenchmarkId());
        assertNull(benchmark.getBenchmarkName());
        assertNull(benchmark.getBenchmarkType());
        assertNull(benchmark.getBenchmarkProvider());
        assertNull(benchmark.getAccessionNumber());
    }

    @Test
    void testSettersAndGetters_RoundTrip() {
        BenchmarkMaster benchmark = new BenchmarkMaster();

        benchmark.setBenchmarkId(101);
        benchmark.setBenchmarkName("S&P 500 Index");
        benchmark.setBenchmarkType("INDEX");
        benchmark.setBenchmarkProvider("Vanguard");
        benchmark.setAccessionNumber("ACC12345");

        assertEquals(101, benchmark.getBenchmarkId());
        assertEquals("S&P 500 Index", benchmark.getBenchmarkName());
        assertEquals("INDEX", benchmark.getBenchmarkType());
        assertEquals("Vanguard", benchmark.getBenchmarkProvider());
        assertEquals("ACC12345", benchmark.getAccessionNumber());
    }

    @Test
    void testInheritedBaseModelFields_AreAccessible() {
        BenchmarkMaster benchmark = new BenchmarkMaster();

        benchmark.setCreatedBy("TEST");

        assertEquals("TEST", benchmark.getCreatedBy());
    }
}
