package funds.configuration.batch;

import funds.model.FundImportPayload;
import funds.model.RawFundSource;
import funds.processor.FundsItemProcessor;
import funds.reader.FundsImportReader;
import funds.tasklet.InitReportingQuarterTasklet;
import funds.writer.FundsItemWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class FundsImportJobConfig {

    private static final int CHUNK_SIZE = 500;

    /**
     * Executes Step 1 (seeds reporting_quarter calendar dimension) followed by
     * Step 2 (populates benchmark_master) followed by
     * Step 3 (populates fund_master, associations, and quarterly cohorts).
     */
    @Bean
    public Job fundsImportJob(
            JobRepository jobRepository,
            @Qualifier("initReportingQuarterStep") Step initReportingQuarterStep,
            @Qualifier("benchmarkExtractionStep") Step benchmarkExtractionStep,
            @Qualifier("fundsImportStep") Step fundsImportStep
    ) {
        return new JobBuilder("fundsImportJob", jobRepository)
                .start(initReportingQuarterStep)
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
}