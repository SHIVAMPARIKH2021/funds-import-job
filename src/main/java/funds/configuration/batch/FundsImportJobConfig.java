package funds.configuration.batch;

import funds.model.FundImportPayload;
import funds.model.RawFundSource;
import funds.processor.FundsItemProcessor;
import funds.reader.FundsImportReader;
import funds.tasklet.InitReportingQuarterTasklet;
import funds.tasklet.CleanStagedFilingBenchmarkTasklet;
import funds.writer.FundsItemWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class FundsImportJobConfig {

    private static final int CHUNK_SIZE = 500;

    /**
     * Pipeline Execution Order:
     * Step 1: Seeds reporting_quarter calendar dimension.
     * Step 2: Extracts benchmark candidates from numeric_facts (defined in BenchmarkProviderConfig).
     * Step 3: Populates fund_master, benchmark associations, and quarterly cohorts.
     * Step 4: Fallback step: Resolves unmapped benchmarks from text_facts where fund_master.benchmark_id IS NULL.
     */
    @Bean
    public Job fundsImportJob(
            JobRepository jobRepository,
            Step initReportingQuarterStep,
            Step benchmarkExtractionStep,
            Step fundsImportStep,
            Step cleanStagingStep
    ) {
        return new JobBuilder("fundsImportJob", jobRepository)
                .start(initReportingQuarterStep)
                .next(cleanStagingStep)
                .next(benchmarkExtractionStep)
                .next(fundsImportStep)
                .build();
    }

    @Bean
    public Step initReportingQuarterStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            InitReportingQuarterTasklet initReportingQuarterTasklet
    ) {
        return new StepBuilder("initReportingQuarterStep", jobRepository)
                .tasklet(initReportingQuarterTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step fundsImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FundsImportReader fundsImportReader,
            FundsItemProcessor fundsItemProcessor,
            FundsItemWriter fundsItemWriter
    ) {
        return new StepBuilder("fundsImportStep", jobRepository)
                .<RawFundSource, FundImportPayload>chunk(CHUNK_SIZE, transactionManager)
                .reader(fundsImportReader)
                .processor(fundsItemProcessor)
                .writer(fundsItemWriter)
                .build();
    }

    @Bean
    public Step cleanStagingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            CleanStagedFilingBenchmarkTasklet cleanStagedFilingBenchmarkTasklet
    ) {
        return new StepBuilder("cleanStagedBenchmarksStep", jobRepository)
                .tasklet(cleanStagedFilingBenchmarkTasklet, transactionManager)
                .build();
    }
}