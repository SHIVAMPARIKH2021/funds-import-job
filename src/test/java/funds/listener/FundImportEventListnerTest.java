package funds.listener;

import funds.dto.EtlCompletedEvent;
import funds.exception.NonRetryableContractException;
import funds.service.JobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundImportEventListenerTest {

    @Mock
    private JobService fundImportService;

    @InjectMocks
    private FundImportEventListener listener;

    private EtlCompletedEvent createValidEvent() {
        return new EtlCompletedEvent(
                "1.0",
                "job-123",
                "com.sec.pipeline.fund.etl.completed",
                Instant.now(),
                "fund-etl:sec-edgar",
                "corr-456",
                new EtlCompletedEvent.FundPayload(
                        "2026",
                        "1",
                        false,
                        150L,
                        null,
                        "SUCCESS"
                )
        );
    }

    @Test
    @DisplayName("Should successfully delegate to JobService and complete when payload is valid")
    void testOnFundImportTrigger_Success() throws Exception {
        EtlCompletedEvent event = createValidEvent();
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(fundImportService.processJob(event)).thenReturn(jobExecution);

        assertDoesNotThrow(() -> listener.onFundImportTrigger(event));

        verify(fundImportService).processJob(event);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void testOnFundImportTrigger_BlankOrNullJobId_ThrowsNonRetryableContractException(String invalidJobId) {
        EtlCompletedEvent event = new EtlCompletedEvent(
                "1.0",
                invalidJobId,
                "com.sec.pipeline.fund.etl.completed",
                Instant.now(),
                "fund-etl",
                "corr-456",
                new EtlCompletedEvent.FundPayload("2026", "1", false, 100L, null, "SUCCESS")
        );

        NonRetryableContractException exception = assertThrows(
                NonRetryableContractException.class,
                () -> listener.onFundImportTrigger(event)
        );

        assertEquals("Payload validation failed: 'jobId' cannot be blank", exception.getMessage());
        verifyNoInteractions(fundImportService);
    }

    @Test
    void testOnFundImportTrigger_NullPayload_ThrowsNonRetryableContractException() {
        EtlCompletedEvent event = new EtlCompletedEvent(
                "1.0",
                "job-123",
                "com.sec.pipeline.fund.etl.completed",
                Instant.now(),
                "fund-etl",
                "corr-456",
                null
        );

        NonRetryableContractException exception = assertThrows(
                NonRetryableContractException.class,
                () -> listener.onFundImportTrigger(event)
        );

        assertEquals("Unsupported dataset payload: null", exception.getMessage());
        verifyNoInteractions(fundImportService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testOnFundImportTrigger_NullOrEmptyQuarter_ThrowsNonRetryableContractException(String invalidQuarter) {
        EtlCompletedEvent.FundPayload payload = new EtlCompletedEvent.FundPayload(
                "2026",
                invalidQuarter,
                false,
                100L,
                null,
                "SUCCESS"
        );
        EtlCompletedEvent event = new EtlCompletedEvent(
                "1.0",
                "job-123",
                "com.sec.pipeline.fund.etl.completed",
                Instant.now(),
                "fund-etl",
                "corr-456",
                payload
        );

        NonRetryableContractException exception = assertThrows(
                NonRetryableContractException.class,
                () -> listener.onFundImportTrigger(event)
        );

        assertEquals("Unsupported dataset payload: " + payload, exception.getMessage());
        verifyNoInteractions(fundImportService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testOnFundImportTrigger_NullOrEmptyYear_ThrowsNonRetryableContractException(String invalidYear) {
        EtlCompletedEvent.FundPayload payload = new EtlCompletedEvent.FundPayload(
                invalidYear,
                "1",
                false,
                100L,
                null,
                "SUCCESS"
        );
        EtlCompletedEvent event = new EtlCompletedEvent(
                "1.0",
                "job-123",
                "com.sec.pipeline.fund.etl.completed",
                Instant.now(),
                "fund-etl",
                "corr-456",
                payload
        );

        NonRetryableContractException exception = assertThrows(
                NonRetryableContractException.class,
                () -> listener.onFundImportTrigger(event)
        );

        assertEquals("Unsupported dataset payload: " + payload, exception.getMessage());
        verifyNoInteractions(fundImportService);
    }

    @Test
    void testOnFundImportTrigger_BatchJobFails_ThrowsIllegalStateException() throws Exception {
        EtlCompletedEvent event = createValidEvent();
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(fundImportService.processJob(event)).thenReturn(jobExecution);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> listener.onFundImportTrigger(event)
        );

        assertEquals("Batch job failed or stopped with status: FAILED", exception.getMessage());
        verify(fundImportService).processJob(event);
    }

    @Test
    void testOnFundImportTrigger_BatchJobStopped_ThrowsIllegalStateException() throws Exception {
        EtlCompletedEvent event = createValidEvent();
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STOPPED);
        when(fundImportService.processJob(event)).thenReturn(jobExecution);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> listener.onFundImportTrigger(event)
        );

        assertEquals("Batch job failed or stopped with status: STOPPED", exception.getMessage());
        verify(fundImportService).processJob(event);
    }
}