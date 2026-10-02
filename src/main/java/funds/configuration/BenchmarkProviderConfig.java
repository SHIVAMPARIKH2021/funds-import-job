package funds.configuration;

import funds.model.BenchmarkMaster;
import funds.model.RawBenchmarkSource;
import funds.processor.BenchmarkItemProcessor;
import funds.model.BenchmarkMaster;
import funds.reader.BenchmarkItemReader;
import funds.writer.BenchmarkItemWriter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
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
            BenchmarkItemReader benchmarkItemReader,
            BenchmarkItemProcessor benchmarkItemProcessor,
            BenchmarkItemWriter benchmarkItemWriter) {
        return new StepBuilder("benchmarkExtractionStep", jobRepository)
                .<RawBenchmarkSource, BenchmarkMaster>chunk(CHUNK_SIZE, transactionManager)
                .reader(benchmarkItemReader)
                .processor(benchmarkItemProcessor)
                .writer(benchmarkItemWriter)
                .build();
    }
}