package funds.writer;

import funds.model.BenchmarkMaster;
import funds.repository.AnalyticsRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class BenchmarkItemWriter implements ItemWriter<BenchmarkMaster> {

    private final AnalyticsRepository analyticsRepository;

    public BenchmarkItemWriter(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public void write(Chunk<? extends BenchmarkMaster> chunk) {
        if (!chunk.isEmpty()) {
            analyticsRepository.saveAllBenchmarks(chunk.getItems());
        }
    }
}