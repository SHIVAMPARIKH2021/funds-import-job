package funds;

import funds.service.JobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
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

    @Test
    @DisplayName("run should do nothing and stay idle when CLI properties are absent")
    void testRun_WhenPropertiesNotPresent_DoesNotTriggerJob() throws Exception {
        when(environment.containsProperty("year")).thenReturn(false);

        Application app = new Application(Map.of("fundImportService", fundImportService), environment);
        app.run();

        verifyNoInteractions(fundImportService);
    }

    @Test
    @DisplayName("run should execute processJob when year and quarter properties are present")
    void testRun_WhenPropertiesPresent_ExecutesJobSuccessfully() throws Exception {
        when(environment.containsProperty("year")).thenReturn(true);
        when(environment.containsProperty("quarter")).thenReturn(true);

        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        when(fundImportService.processJob()).thenReturn(jobExecution);

        Application app = new Application(Map.of("fundImportService", fundImportService), environment);
        assertDoesNotThrow(() -> app.run());

        verify(fundImportService).processJob();
    }

    @Test
    @DisplayName("run should throw IllegalStateException when fundImportService bean is missing from map")
    void testRun_WhenBeanNotFound_ThrowsIllegalStateException() {
        when(environment.containsProperty("year")).thenReturn(true);
        when(environment.containsProperty("quarter")).thenReturn(true);

        Application app = new Application(Collections.emptyMap(), environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, app::run);
        assertEquals("Could not find bean 'fundImportService'", ex.getMessage());
    }

    @Test
    @DisplayName("run should throw IllegalStateException when JobExecution does not exit with COMPLETED")
    void testRun_WhenJobExecutionFails_ThrowsIllegalStateException() throws Exception {
        when(environment.containsProperty("year")).thenReturn(true);
        when(environment.containsProperty("quarter")).thenReturn(true);

        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.FAILED);
        when(fundImportService.processJob()).thenReturn(jobExecution);

        Application app = new Application(Map.of("fundImportService", fundImportService), environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, app::run);
        assertEquals("Job execution failed: exitCode=FAILED;exitDescription=", ex.getMessage());
        verify(fundImportService).processJob();
    }
}