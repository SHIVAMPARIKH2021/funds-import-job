package funds.configuration;

import funds.model.FundMaster;
import funds.model.RawFundSource;
import funds.processor.FundsItemProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
public class FundsImportJobConfig {

    private static final int CHUNK_SIZE = 500;

    /**
     * Executes Step 1 (populates benchmark_master) followed by
     * Step 2 (populates fund_master with valid foreign keys).
     */
    @Bean
    public Job fundsImportJob(
            JobRepository jobRepository,
            @Qualifier("benchmarkExtractionStep") Step benchmarkExtractionStep,
            @Qualifier("fundsImportStep") Step fundsImportStep
    ) {
        return new JobBuilder("fundsImportJob", jobRepository)
                .start(benchmarkExtractionStep)
                .next(fundsImportStep)
                .build();
    }

    @Bean
    public Step fundsImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("fundsImportReader")ItemReader<RawFundSource> fundsImportReader,
            @Qualifier("fundsItemProcessor")ItemProcessor<? super RawFundSource,
                    ? extends FundMaster> fundsItemProcessor,
            @Qualifier("fundsItemWriter")ItemWriter<FundMaster> fundsItemWriter
    ) {
        return new StepBuilder("fundsImportStep", jobRepository)
                .<RawFundSource, FundMaster>chunk(CHUNK_SIZE, transactionManager)
                .reader(fundsImportReader)
                .processor(fundsItemProcessor)
                .writer(fundsItemWriter)
                .build();
    }
}