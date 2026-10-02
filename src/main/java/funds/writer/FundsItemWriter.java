package funds.writer;

import funds.model.FundMaster;
import funds.repository.AnalyticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FundsItemWriter implements ItemWriter<FundMaster> {

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Override
    public void write(Chunk<? extends FundMaster> chunk) throws Exception {
        List<? extends FundMaster> items = chunk.getItems();

        if (items.isEmpty()) {
            return;
        }

        log.debug("Persisting chunk of {} fund records to analytics.fund_master", items.size());

        analyticsRepository.saveAllFunds(items);

        log.info("Successfully persisted {} fund records", items.size());
    }
}