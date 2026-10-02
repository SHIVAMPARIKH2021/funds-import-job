package funds.writer;

import funds.model.BenchmarkMaster;
import funds.repository.AnalyticsRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@StepScope
public class BenchmarkItemWriter implements ItemWriter<BenchmarkMaster> {

    @Autowired
    private final AnalyticsRepository analyticsRepository;

    public BenchmarkItemWriter(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public void write(Chunk<? extends BenchmarkMaster> chunk) throws Exception {
        analyticsRepository.saveAllBenchmarks(chunk.getItems());
    }
}