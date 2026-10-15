package funds.writer;

import funds.model.FundBenchmarkAssociation;
import funds.model.FundImportPayload;
import funds.model.FundMaster;
import funds.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.Chunk;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class FundsItemWriterTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private FundsItemWriter fundsItemWriter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private FundMaster fund(String seriesId) {
        FundMaster fundMaster = new FundMaster();
        fundMaster.setSeriesId(seriesId);
        return fundMaster;
    }

    private FundBenchmarkAssociation association(String seriesId) {
        return new FundBenchmarkAssociation(seriesId, 1, "Benchmark1", true, "ACC12345");
    }

    @Test
    void testWrite_EmptyChunk_DoesNotTouchRepository() throws Exception {
        fundsItemWriter.write(Chunk.<FundImportPayload>of());

        verifyNoInteractions(analyticsRepository);
    }

    @Test
    void testWrite_SavesFundsAndUpsertsAssociations() throws Exception {
        FundMaster fundMaster = fund("TEST123");
        FundBenchmarkAssociation association = association("TEST123");
        FundImportPayload payload = new FundImportPayload(fundMaster, Collections.singletonList(association));

        fundsItemWriter.write(Chunk.of(payload));

        verify(analyticsRepository).saveAllFunds(Collections.singletonList(fundMaster));
        verify(analyticsRepository).upsertFundBenchmarkAssociations(Collections.singletonList(association));
    }

    @Test
    void testWrite_PayloadWithoutAssociations_OnlySavesFunds() throws Exception {
        FundMaster fundMaster = fund("TEST123");
        FundImportPayload payload = new FundImportPayload(fundMaster, Collections.emptyList());

        fundsItemWriter.write(Chunk.of(payload));

        verify(analyticsRepository).saveAllFunds(Collections.singletonList(fundMaster));
        verify(analyticsRepository, never()).upsertFundBenchmarkAssociations(anyList());
    }

    @Test
    void testWrite_PayloadWithNullFundMaster_OnlyUpsertsAssociations() throws Exception {
        FundBenchmarkAssociation association = association("TEST123");
        FundImportPayload payload = new FundImportPayload(null, Collections.singletonList(association));

        fundsItemWriter.write(Chunk.of(payload));

        verify(analyticsRepository, never()).saveAllFunds(anyList());
        verify(analyticsRepository).upsertFundBenchmarkAssociations(Collections.singletonList(association));
    }

    @Test
    void testWrite_PayloadWithNullFundAndNullAssociations_IsSkipped() throws Exception {
        FundImportPayload payload = new FundImportPayload(null, null);

        fundsItemWriter.write(Chunk.of(payload));

        verifyNoInteractions(analyticsRepository);
    }

    @Test
    void testWrite_NullPayloadInChunk_IsSkipped() throws Exception {
        FundMaster fundMaster = fund("TEST123");
        FundImportPayload validPayload = new FundImportPayload(fundMaster, Collections.emptyList());

        fundsItemWriter.write(new Chunk<>(Arrays.asList(validPayload, null)));

        verify(analyticsRepository).saveAllFunds(Collections.singletonList(fundMaster));
        verify(analyticsRepository, never()).upsertFundBenchmarkAssociations(anyList());
    }

    @Test
    void testWrite_MultiplePayloads_PersistsAllFundsAndAssociations() throws Exception {
        FundMaster firstFund = fund("TEST123");
        FundMaster secondFund = fund("TEST456");
        FundBenchmarkAssociation firstAssociation = association("TEST123");
        FundBenchmarkAssociation secondAssociation = new FundBenchmarkAssociation(
                "TEST456", 2, "Benchmark2", false, "ACC67890");

        fundsItemWriter.write(Chunk.of(
                new FundImportPayload(firstFund, Collections.singletonList(firstAssociation)),
                new FundImportPayload(secondFund, Collections.singletonList(secondAssociation))));

        verify(analyticsRepository).saveAllFunds(Arrays.asList(firstFund, secondFund));
        verify(analyticsRepository).upsertFundBenchmarkAssociations(
                Arrays.asList(firstAssociation, secondAssociation));
    }
}
