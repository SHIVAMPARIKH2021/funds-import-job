package funds.writer;

import funds.model.FundBenchmarkAssociation;
import funds.model.FundImportPayload;
import funds.model.FundMaster;
import funds.model.QuarterFundAssociation;
import funds.repository.AnalyticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@StepScope
public class FundsItemWriter implements ItemWriter<FundImportPayload> {

    private final AnalyticsRepository analyticsRepository;
    private final int quarterId;
    private final String executor;

    public FundsItemWriter(
            AnalyticsRepository analyticsRepository,
            @Value("#{jobExecutionContext['quarterId']}") int quarterId,
            @Value("#{jobParameters['executor']}") String executor) {
        this.analyticsRepository = analyticsRepository;
        this.quarterId = quarterId;
        this.executor = (executor != null && !executor.isBlank()) ? executor.trim() : "System";
    }

    @Override
    public void write(Chunk<? extends FundImportPayload> chunk) {
        if (chunk.isEmpty()) {
            return;
        }

        List<FundMaster> fundsToUpsert = new ArrayList<>();
        List<FundBenchmarkAssociation> associationsToUpsert = new ArrayList<>();
        List<QuarterFundAssociation> quarterAssociationsToUpsert = new ArrayList<>();

        for (FundImportPayload payload : chunk) {
            if (payload != null && payload.getFundMaster() != null) {
                FundMaster fund = payload.getFundMaster();
                fundsToUpsert.add(fund);

                if (payload.getAssociations() != null && !payload.getAssociations().isEmpty()) {
                    associationsToUpsert.addAll(payload.getAssociations());
                }

                quarterAssociationsToUpsert.add(new QuarterFundAssociation(
                        this.quarterId,
                        fund.getSeriesId(),
                        fund.getAccessionNumber(),
                        this.executor
                ));
            }
        }

        if (!fundsToUpsert.isEmpty()) {
            log.debug("Persisting chunk of {} fund records to analytics.fund_master", fundsToUpsert.size());
            analyticsRepository.saveAllFunds(fundsToUpsert);
            log.info("Successfully persisted {} fund records", fundsToUpsert.size());
        }

        if (!associationsToUpsert.isEmpty()) {
            log.debug("Persisting chunk of {} records to analytics.fund_benchmark_association", associationsToUpsert.size());
            analyticsRepository.upsertFundBenchmarkAssociations(associationsToUpsert);
            log.info("Successfully persisted {} fund_benchmark_association records", associationsToUpsert.size());
        }

        if (!quarterAssociationsToUpsert.isEmpty()) {
            log.debug("Persisting chunk of {} records to analytics.quarter_fund_association", quarterAssociationsToUpsert.size());
            analyticsRepository.upsertQuarterFundAssociations(quarterAssociationsToUpsert);
            log.info("Successfully persisted {} quarter_fund_association records", quarterAssociationsToUpsert.size());
        }
    }
}