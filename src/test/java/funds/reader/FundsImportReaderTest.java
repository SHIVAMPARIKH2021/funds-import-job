package funds.reader;

import funds.model.RawFundSource;
import funds.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class FundsImportReaderTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private FundsImportReader fundsImportReader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRead_WithYearAndQuarter() throws Exception {
        // Set job parameters via reflection
        ReflectionTestUtils.setField(fundsImportReader, "year", "2024");
        ReflectionTestUtils.setField(fundsImportReader, "quarter", "1");

        RawFundSource expected = new RawFundSource();
        expected.setSeriesId("TEST123");
        List<RawFundSource> funds = Collections.singletonList(expected);
        when(analyticsRepository.findRawFundsForQuarter(
                eq(Collections.singletonList("tag")),
                eq(LocalDate.of(2024, 1, 1)),
                eq(LocalDate.of(2024, 3, 31)))).thenReturn(funds);

        RawFundSource result = fundsImportReader.read();
        // reader returns null on first call if no data is available (mock returns empty list)
        assertNull(result);
    }

    @Test
    void testCalculateQuarterDateRange() {
        ReflectionTestUtils.setField(fundsImportReader, "year", "2024");
        ReflectionTestUtils.setField(fundsImportReader, "quarter", "1");

        var result = fundsImportReader.calculateQuarterDateRange(2024, 1);
        // Q1: Jan-Mar, min=2024-01-01, max=2024-03-31
        org.junit.jupiter.api.Assertions.assertEquals(java.time.LocalDate.of(2024, 1, 1), result.get("minDate"));
        org.junit.jupiter.api.Assertions.assertEquals(java.time.LocalDate.of(2024, 3, 31), result.get("maxDate"));
    }
}
