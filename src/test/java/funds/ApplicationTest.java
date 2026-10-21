package funds;

import funds.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationTest {

    @Mock
    private JobService fundImportService;

    @Mock
    private Environment environment;

    private Application app;

    @BeforeEach
    void setUp() {
        app = new Application();
    }

    @Test
    @DisplayName("manualBatchRunner should execute processJob when YEAR and QUARTER properties are present")
    void testRun_WhenPropertiesPresent_ExecutesJobSuccessfully() throws Exception {
        when(environment.getProperty("YEAR")).thenReturn("2026");
        when(environment.getProperty("QUARTER")).thenReturn("1");

        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        when(fundImportService.processJob()).thenReturn(jobExecution);

        CommandLineRunner runner = app.manualBatchRunner(Map.of("fundImportService", fundImportService), environment);

        assertDoesNotThrow(() -> runner.run());

        verify(fundImportService).processJob();
    }

    @Test
    @DisplayName("manualBatchRunner should throw IllegalStateException when fundImportService bean is missing from map")
    void testRun_WhenBeanNotFound_ThrowsIllegalStateException() {
        when(environment.getProperty("YEAR")).thenReturn("2026");
        when(environment.getProperty("QUARTER")).thenReturn("1");

        CommandLineRunner runner = app.manualBatchRunner(Collections.emptyMap(), environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run());
        assertEquals("Could not find bean 'fundImportService'", ex.getMessage());
        verifyNoInteractions(fundImportService);
    }

    @Test
    @DisplayName("manualBatchRunner should throw IllegalStateException when JobExecution does not exit with COMPLETED")
    void testRun_WhenJobExecutionFails_ThrowsIllegalStateException() throws Exception {
        when(environment.getProperty("YEAR")).thenReturn("2026");
        when(environment.getProperty("QUARTER")).thenReturn("1");

        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.FAILED);
        when(fundImportService.processJob()).thenReturn(jobExecution);

        CommandLineRunner runner = app.manualBatchRunner(Map.of("fundImportService", fundImportService), environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run());
        assertEquals("Job execution failed: exitCode=FAILED;exitDescription=", ex.getMessage());
        verify(fundImportService).processJob();
    }
}