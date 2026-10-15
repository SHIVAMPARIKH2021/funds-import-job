package funds.service;

import funds.exception.InvalidParameterException;
import funds.service.impl.FundImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FundImportServiceTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job fundsImportJob;

    @InjectMocks
    private FundImportService fundImportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // The @Value-injected properties are not resolved in plain Mockito unit tests
        ReflectionTestUtils.setField(fundImportService, "quarter", "Q1");
        ReflectionTestUtils.setField(fundImportService, "year", "2025");
        ReflectionTestUtils.setField(fundImportService, "executor", "TEST");
        ReflectionTestUtils.setField(fundImportService, "dryRun", "false");
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
        assertEquals("SYSTEM", parametersCaptor.getValue().getString("executor"));
    }

    @Test
    void testProcessJob_WithNullExecutor_DefaultsToSystem() throws Exception {
        ReflectionTestUtils.setField(fundImportService, "executor", null);
        when(jobLauncher.run(eq(fundsImportJob), any(JobParameters.class)))
                .thenReturn(new JobExecution(0L));

        fundImportService.processJob();

        ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(fundsImportJob), parametersCaptor.capture());
        assertEquals("SYSTEM", parametersCaptor.getValue().getString("executor"));
    }

    @Test
    void testProcessJob_WhenJobLauncherThrows_ExceptionPropagates() throws Exception {
        when(jobLauncher.run(eq(fundsImportJob), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("Job is already running"));

        assertThrows(JobExecutionAlreadyRunningException.class, () -> fundImportService.processJob());
    }
}