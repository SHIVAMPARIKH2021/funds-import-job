package funds.writer;

import funds.model.BenchmarkMaster;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@StepScope
public class BenchmarkItemWriter implements ItemWriter<BenchmarkMaster> {

    private final JdbcBatchItemWriter<BenchmarkMaster> delegate;

    public BenchmarkItemWriter(
            DataSource dataSource,
            @Qualifier("upsertBenchmarkMasterQuery") String upsertBenchmarkMasterQuery
    ) {
        this.delegate = new JdbcBatchItemWriterBuilder<BenchmarkMaster>()
                .dataSource(dataSource)
                .sql(upsertBenchmarkMasterQuery)
                .beanMapped()
                .build();
        this.delegate.afterPropertiesSet();
    }

    @Override
    public void write(Chunk<? extends BenchmarkMaster> chunk) throws Exception {
        this.delegate.write(chunk);
    }
}