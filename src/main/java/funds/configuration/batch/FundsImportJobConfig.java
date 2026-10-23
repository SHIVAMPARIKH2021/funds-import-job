package funds.configuration.batch;

import funds.model.BenchmarkMaster;
import funds.model.FundImportPayload;
import funds.model.RawBenchmarkSource;
import funds.model.RawFundSource;
import funds.processor.BenchmarkItemProcessor;
import funds.processor.FundsItemProcessor;
import funds.reader.BenchmarkItemReader;
import funds.reader.FundsImportReader;
import funds.reader.TextBenchmarkItemReader;
import funds.tasklet.InitReportingQuarterTasklet;
import funds.writer.BenchmarkItemWriter;
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
     * Pipeline Execution Order:
     * Step 1: Seeds reporting_quarter calendar dimension.
     * Step 2: Extracts benchmark candidates from numeric_facts + presentation_labels.
     * Step 3: Populates fund_master, benchmark associations, and quarterly cohorts.
     * Step 4: Fallback step: Resolves unmapped benchmarks from text_facts where fund_master.benchmark_id IS NULL.
     */
    @Bean
    public Job fundsImportJob(
            JobRepository jobRepository,
            @Qualifier("initReportingQuarterStep") Step initReportingQuarterStep,
            @Qualifier("benchmarkExtractionStep") Step benchmarkExtractionStep,
            @Qualifier("fundsImportStep") Step fundsImportStep,
            @Qualifier("textBenchmarkFallbackStep") Step textBenchmarkFallbackStep
    ) {
        return new JobBuilder("fundsImportJob", jobRepository)
                .start(initReportingQuarterStep)
                .next(benchmarkExtractionStep)
                .next(fundsImportStep)
                .next(textBenchmarkFallbackStep)
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
    public Step benchmarkExtractionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            BenchmarkItemReader numericBenchmarkReader,
            BenchmarkItemProcessor benchmarkProcessor,
            BenchmarkItemWriter benchmarkWriter
    ) {
        return new StepBuilder("benchmarkExtractionStep", jobRepository)
                .<RawBenchmarkSource, BenchmarkMaster>chunk(CHUNK_SIZE, transactionManager)
                .reader(numericBenchmarkReader)
                .processor(benchmarkProcessor)
                .writer(benchmarkWriter)
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
    public Step textBenchmarkFallbackStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TextBenchmarkItemReader textReader,
            BenchmarkItemProcessor benchmarkProcessor,
            BenchmarkItemWriter benchmarkWriter
    ) {
        return new StepBuilder("textBenchmarkFallbackStep", jobRepository)
                .<RawBenchmarkSource, BenchmarkMaster>chunk(CHUNK_SIZE, transactionManager)
                .reader(textReader)
                .processor(benchmarkProcessor)
                .writer(benchmarkWriter)
                .build();
    }
}