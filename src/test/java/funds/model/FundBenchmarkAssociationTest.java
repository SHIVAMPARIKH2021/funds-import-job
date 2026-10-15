package funds.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FundBenchmarkAssociationTest {

    @Test
    void testNoArgsConstructor_DefaultsIsPrimaryToFalse() {
        FundBenchmarkAssociation association = new FundBenchmarkAssociation();

        assertEquals(false, association.getIsPrimary());
        assertNull(association.getId());
        assertNull(association.getSeriesId());
        assertNull(association.getBenchmarkId());
        assertNull(association.getBenchmarkName());
        assertNull(association.getAccessionNumber());
    }

    @Test
    void testConvenienceConstructor_SetsAllFieldsExceptId() {
        FundBenchmarkAssociation association = new FundBenchmarkAssociation(
                "TEST123", 1, "Benchmark1", true, "ACC12345");

        assertNull(association.getId());
        assertEquals("TEST123", association.getSeriesId());
        assertEquals(1, association.getBenchmarkId());
        assertEquals("Benchmark1", association.getBenchmarkName());
        assertEquals(true, association.getIsPrimary());
        assertEquals("ACC12345", association.getAccessionNumber());
    }

    @Test
    void testAllArgsConstructor_SetsAllFieldsIncludingId() {
        FundBenchmarkAssociation association = new FundBenchmarkAssociation(
                10, "TEST123", 2, "Benchmark2", false, "ACC67890");

        assertEquals(10, association.getId());
        assertEquals("TEST123", association.getSeriesId());
        assertEquals(2, association.getBenchmarkId());
        assertEquals("Benchmark2", association.getBenchmarkName());
        assertEquals(false, association.getIsPrimary());
        assertEquals("ACC67890", association.getAccessionNumber());
    }

    @Test
    void testSettersAndGetters_RoundTrip() {
        FundBenchmarkAssociation association = new FundBenchmarkAssociation();

        association.setId(1);
        association.setSeriesId("TEST123");
        association.setBenchmarkId(3);
        association.setBenchmarkName("Benchmark3");
        association.setIsPrimary(true);
        association.setAccessionNumber("ACC11111");

        assertEquals(1, association.getId());
        assertEquals("TEST123", association.getSeriesId());
        assertEquals(3, association.getBenchmarkId());
        assertEquals("Benchmark3", association.getBenchmarkName());
        assertEquals(true, association.getIsPrimary());
        assertEquals("ACC11111", association.getAccessionNumber());
    }
}
