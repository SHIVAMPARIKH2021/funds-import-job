package funds.tasklet;

import funds.repository.AnalyticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class InitReportingQuarterTasklet implements Tasklet {

    private final AnalyticsRepository analyticsRepository;
    private final String year;
    private final String quarter;
    private final String executor;

    public InitReportingQuarterTasklet(
            AnalyticsRepository analyticsRepository,
            @Value("#{jobParameters['year']}") String year,
            @Value("#{jobParameters['quarter']}") String quarter,
            @Value("#{jobParameters['executor']}") String executor) {
        this.analyticsRepository = analyticsRepository;
        this.year = year;
        this.quarter = quarter;
        this.executor = (executor != null && !executor.isBlank()) ? executor.trim() : "System";
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        int filingYear = Integer.parseInt(this.year.trim());
        int filingQuarter = Integer.parseInt(this.quarter.trim());

        log.info("Initializing reporting_quarter dimension for Year: {}, Quarter: {}, Executor: {}",
                filingYear, filingQuarter, this.executor);

        int quarterId = analyticsRepository.getOrCreateReportingQuarter(filingYear, filingQuarter, this.executor);

        // Promote quarterId to JobExecutionContext so downstream steps can access it via SpEL
        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .putInt("quarterId", quarterId);

        log.info("Resolved reporting_quarter dimension [quarterId: {}]", quarterId);

        return RepeatStatus.FINISHED;
    }
}