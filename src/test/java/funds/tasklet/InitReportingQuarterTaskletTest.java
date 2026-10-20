package funds.tasklet;

import funds.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitReportingQuarterTaskletTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    @Mock
    private StepContext stepContext;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private JobExecution jobExecution;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        executionContext = new ExecutionContext();
    }

    private void mockSpringBatchContext() {
        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
    }

    @Test
    void testExecute_Success() throws Exception {
        mockSpringBatchContext();
        when(analyticsRepository.getOrCreateReportingQuarter(2026, 1, "fund-etl")).thenReturn(42);

        InitReportingQuarterTasklet tasklet = new InitReportingQuarterTasklet(
                analyticsRepository,
                "2026",
                "1",
                "fund-etl"
        );

        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        assertEquals(RepeatStatus.FINISHED, status);
        assertEquals(42, executionContext.getInt("quarterId"));
        verify(analyticsRepository).getOrCreateReportingQuarter(2026, 1, "fund-etl");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void testExecute_BlankExecutor_DefaultsToSystem(String blankExecutor) throws Exception {
        mockSpringBatchContext();
        when(analyticsRepository.getOrCreateReportingQuarter(2026, 2, "System")).thenReturn(99);

        InitReportingQuarterTasklet tasklet = new InitReportingQuarterTasklet(
                analyticsRepository,
                "  2026 ",
                " 2 ",
                blankExecutor
        );

        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        assertEquals(RepeatStatus.FINISHED, status);
        assertEquals(99, executionContext.getInt("quarterId"));
        verify(analyticsRepository).getOrCreateReportingQuarter(2026, 2, "System");
    }

    @Test
    void testExecute_InvalidNumberFormat_ThrowsException() {
        InitReportingQuarterTasklet tasklet = new InitReportingQuarterTasklet(
                analyticsRepository,
                "invalidYear",
                "1",
                "System"
        );

        assertThrows(NumberFormatException.class, () -> tasklet.execute(stepContribution, chunkContext));
    }
}