package funds.writer;

import funds.model.FundBenchmarkAssociation;
import funds.model.FundImportPayload;
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
public class FundsItemWriter implements ItemWriter<FundImportPayload> {

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Override
    public void write(Chunk<? extends FundImportPayload> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        List<FundMaster> fundsToUpsert = new ArrayList<>();
        List<FundBenchmarkAssociation> associationsToUpsert = new ArrayList<>();

        for (FundImportPayload payload : chunk) {
            if (payload != null) {
                if (payload.getFundMaster() != null) {
                    fundsToUpsert.add(payload.getFundMaster());
                }
                if (payload.getAssociations() != null && !payload.getAssociations().isEmpty()) {
                    associationsToUpsert.addAll(payload.getAssociations());
                }
            }
        }

        log.debug("Persisting chunk of {} fund records to analytics.fund_master", chunk.size());

        if (!fundsToUpsert.isEmpty()) {
            analyticsRepository.saveAllFunds(fundsToUpsert);
        }
        log.info("Successfully persisted {} fund records", chunk.size());

        log.debug("Persisting chunk of {} fund records to analytics.fund_benchmark_association", chunk.size());

        if (!associationsToUpsert.isEmpty()) {
            analyticsRepository.upsertFundBenchmarkAssociations(associationsToUpsert);
        }
        log.info("Successfully persisted {} fund_benchmark_association records", chunk.size());
    }
}