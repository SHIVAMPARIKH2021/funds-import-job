package funds.writer;

import funds.model.FundMaster;
import funds.repository.AnalyticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@StepScope
public class FundsItemWriter implements ItemWriter<List<FundMaster>> {

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Override
    public void write(Chunk<? extends List<FundMaster>> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }
        // Flatten the chunk of lists into a single batch list for JDBC execution
        List<FundMaster> flatList = new ArrayList<>();
        for (List<FundMaster> fundList : chunk) {
            if (fundList != null && !fundList.isEmpty()) {
                flatList.addAll(fundList);
            }
        }

        log.debug("Persisting chunk of {} fund records to analytics.fund_master", chunk.size());

        if (!flatList.isEmpty()) {
            analyticsRepository.saveAllFunds(flatList);
        }
        log.info("Successfully persisted {} fund records", chunk.size());
    }
}