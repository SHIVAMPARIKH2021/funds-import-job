package funds.configuration;

import funds.model.BenchmarkMaster;
import funds.processor.BenchmarkItemProcessor;
import funds.model.BenchmarkMaster;
import funds.writer.BenchmarkItemWriter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BenchmarkProviderConfig {

    private static final int CHUNK_SIZE = 500;

    @Bean
    public Step benchmarkExtractionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<? extends java.lang.String> benchmarkItemReader,
            ItemProcessor<? super String, ? extends BenchmarkMaster> benchmarkItemProcessor,
            BenchmarkItemWriter benchmarkItemWriter
    ) {
        return new StepBuilder("benchmarkExtractionStep", jobRepository)
                .<String, BenchmarkMaster>chunk(CHUNK_SIZE, transactionManager)
                .reader(benchmarkItemReader)
                .processor(benchmarkItemProcessor)
                .writer(benchmarkItemWriter)
                .build();
    }
}