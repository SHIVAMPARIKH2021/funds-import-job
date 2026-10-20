package funds.service;

import funds.dto.EtlCompletedEvent;
import funds.exception.InvalidParameterException;
import funds.exception.NonRetryableContractException;
import funds.service.impl.FundImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundImportServiceTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job fundsImportJob;

    @Mock
    private JobExplorer jobExplorer;

    private FundImportService fundImportService;

    @BeforeEach
    void setUp() throws Exception {
        // Instantiate the service with all mocked dependencies first
        fundImportService = new FundImportService(jobLauncher, fundsImportJob, jobExplorer);

        // Populate field-injected values onto the active instance
        ReflectionTestUtils.setField(fundImportService, "quarter", "Q1");
        ReflectionTestUtils.setField(fundImportService, "year", "2025");
        ReflectionTestUtils.setField(fundImportService, "executor", "TEST");
        ReflectionTestUtils.setField(fundImportService, "dryRun", "false");
        ReflectionTestUtils.setField(fundImportService, "force", false);

        // Lenient default stubbing so tests only override what they need
        lenient().when(fundsImportJob.getName()).thenReturn("fundsImportJob");
        lenient().when(jobExplorer.getJobInstanceCount("fundsImportJob")).thenReturn(0L);
    }

    @Test
    void testProcessJob_WithNullQuarter_ThrowsInvalidParameterException() {
        ReflectionTestUtils.setField(fundImportService, "quarter", null);

        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class, () -> fundImportService.processJob());

        assertTrue(exception.getMessage().contains("is invalid"));
        verifyNoInteractions(jobLauncher);
    }

    @Test
    void testProcessJob_WithEmptyQuarter_ThrowsInvalidParameterException() {
        ReflectionTestUtils.setField(fundImportService, "quarter", "");

        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class, () -> fundImportService.processJob());

        assertTrue(exception.getMessage().contains("is invalid"));
        verifyNoInteractions(jobLauncher);
    }

    @Test
    void testProcessJob_WithNullYear_ThrowsInvalidParameterException() {
        ReflectionTestUtils.setField(fundImportService, "year", null);

        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class, () -> fundImportService.processJob());

        assertTrue(exception.getMessage().contains("is invalid"));
        verifyNoInteractions(jobLauncher);
    }

    @Test
    void testProcessJob_WithEmptyYear_ThrowsInvalidParameterException() {
        ReflectionTestUtils.setField(fundImportService, "year", "");

        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class, () -> fundImportService.processJob());

        assertTrue(exception.getMessage().contains("is invalid"));
        verifyNoInteractions(jobLauncher);
    }

    @Test
    void testProcessJob_WithValidParameters_LaunchesJobAndReturnsExecution() throws Exception {
        JobExecution jobExecution = new JobExecution(0L);
        when(jobLauncher.run(eq(fundsImportJob), any(JobParameters.class))).thenReturn(jobExecution);

        JobExecution result = fundImportService.processJob();

        assertSame(jobExecution, result);

        ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(fundsImportJob), parametersCaptor.capture());

        JobParameters parameters = parametersCaptor.getValue();
        assertEquals("TEST", parameters.getString("executor"));
        assertEquals("Q1", parameters.getString("quarter"));
        assertEquals("2025", parameters.getString("year"));
        assertEquals("false", parameters.getString("dryrun"));
        assertNotNull(parameters.getLong("runId"));
        assertTrue(parameters.getLong("runId") > 0);
    }

    @Test
    void testProcessJob_WithBlankExecutor_DefaultsToSystem() throws Exception {
        ReflectionTestUtils.setField(fundImportService, "executor", "   ");
        when(jobLauncher.run(eq(fundsImportJob), any(JobParameters.class)))
                .thenReturn(new JobExecution(0L));

        fundImportService.processJob();

        ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(fundsImportJob), parametersCaptor.capture());
        assertEquals("System", parametersCaptor.getValue().getString("executor"));
    }

    @Test
    void testProcessJob_WithNullExecutor_DefaultsToSystem() throws Exception {
        ReflectionTestUtils.setField(fundImportService, "executor", null);
        when(jobLauncher.run(eq(fundsImportJob), any(JobParameters.class)))
                .thenReturn(new JobExecution(0L));

        fundImportService.processJob();

        ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(fundsImportJob), parametersCaptor.capture());
        assertEquals("System", parametersCaptor.getValue().getString("executor"));
    }

    @Test
    void testProcessJob_WhenJobLauncherThrows_ExceptionPropagates() throws Exception {
        when(jobLauncher.run(eq(fundsImportJob), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("Job is already running"));

        assertThrows(JobExecutionAlreadyRunningException.class, () -> fundImportService.processJob());
    }

    @Test
    void testProcessJob_WhenAlreadyCompleted_ThrowsException() throws Exception {
        ReflectionTestUtils.setField(fundImportService, "year", "2026");
        ReflectionTestUtils.setField(fundImportService, "quarter", "1");
        ReflectionTestUtils.setField(fundImportService, "force", false);

        JobInstance instance = new JobInstance(1L, "fundsImportJob");
        JobExecution completedExecution = new JobExecution(10L, new JobParametersBuilder()
                .addString("year", "2026")
                .addString("quarter", "1")
                .toJobParameters());
        completedExecution.setStatus(BatchStatus.COMPLETED);

        when(jobExplorer.getJobInstanceCount("fundsImportJob")).thenReturn(1L);
        when(jobExplorer.getJobInstances("fundsImportJob", 0, 1)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(completedExecution));

        assertThrows(JobInstanceAlreadyCompleteException.class, () -> fundImportService.processJob());
        verifyNoInteractions(jobLauncher);
    }

    @Test
    void testProcessJobWithEvent_WhenAlreadyCompleted_ThrowsException() throws Exception {
        EtlCompletedEvent event = new EtlCompletedEvent(
                "1.0",
                "123abc",
                "job.completed",
                Instant.now(),
                "fund-etl",
                "id-1",
                new EtlCompletedEvent.FundPayload(
                        "2026",
                        "1",
                        false,
                        1L,
                        "",
                        "Success"
                )
        );
        ReflectionTestUtils.setField(fundImportService, "year", event.payload().year());
        ReflectionTestUtils.setField(fundImportService, "quarter", event.payload().quarter());
        ReflectionTestUtils.setField(fundImportService, "force", event.payload().isForcedImport());

        JobInstance instance = new JobInstance(1L, "fundsImportJob");
        JobExecution completedExecution = new JobExecution(10L, new JobParametersBuilder()
                .addString("year", "2026")
                .addString("quarter", "1")
                .toJobParameters());
        completedExecution.setStatus(BatchStatus.COMPLETED);

        when(jobExplorer.getJobInstanceCount("fundsImportJob")).thenReturn(1L);
        when(jobExplorer.getJobInstances("fundsImportJob", 0, 1)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(completedExecution));

        assertThrows(JobInstanceAlreadyCompleteException.class, () -> fundImportService.processJob(event));
        verifyNoInteractions(jobLauncher);
    }

    @Test
    void testProcessJobWithNullEvent_WhenAlreadyCompleted_ThrowsException() throws Exception {
        assertThrows(NonRetryableContractException.class, () -> fundImportService.processJob(null));
        verifyNoInteractions(jobLauncher);
    }

    @Test
    void testProcessJobWithNullEventPayload_WhenAlreadyCompleted_ThrowsException() throws Exception {
        EtlCompletedEvent event = new EtlCompletedEvent(
                "1.0",
                "123abc",
                "job.completed",
                Instant.now(),
                "fund-etl",
                "id-1",
                null
        );
        assertThrows(NonRetryableContractException.class, () -> fundImportService.processJob(event));
        verifyNoInteractions(jobLauncher);
    }
}