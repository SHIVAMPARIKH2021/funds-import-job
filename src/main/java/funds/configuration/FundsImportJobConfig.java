package funds.configuration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.Writer;

@Configuration
public class FundsImportJobConfig {

    @Bean
    public Job configureJob(JobRepository jobRepository, Step configureStep) {
        System.out.println("Configuring Funds Import Job...");
        return new JobBuilder("fundsImportJob", jobRepository)
                .start(configureStep)
                .build();
    }

    @Bean
    public Step configureStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              ItemReader<String> reader,
                              ItemProcessor<String, String> processor,
                              ItemWriter<String> writer) {
        return new StepBuilder("fundsImportStep", jobRepository)
                .<String, String>chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public ItemReader<String> reader() {
        return new ItemReader<String>() {
            @Override
            public String read() throws Exception {
                // Implement your reading logic here
                return null;
            }
        };
    }

    @Bean
    public ItemProcessor<String, String> processor() {
        return new ItemProcessor<String, String>() {
            @Override
            public String process(String item) throws Exception {
                // Implement your processing logic here
                return item;
            }
        };
    }

    @Bean
    public ItemWriter<String> writer() {
        return new ItemWriter<String>() {
            @Override
            public void write(Chunk<? extends String> chunk) throws Exception {

            }

            public void write(java.util.List<? extends String> items) throws Exception {
                // Implement your writing logic here
            }
        };
    }
}
