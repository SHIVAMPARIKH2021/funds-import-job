package funds.writer;

import funds.model.BenchmarkMaster;
import funds.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.Chunk;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;

class BenchmarkItemWriterTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private BenchmarkItemWriter benchmarkItemWriter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testWrite_DelegatesChunkItemsToRepository() throws Exception {
        BenchmarkMaster first = new BenchmarkMaster();
        first.setBenchmarkName("S&P 500");
        first.setAccessionNumber("ACC12345");
        BenchmarkMaster second = new BenchmarkMaster();
        second.setBenchmarkName("MSCI World");
        second.setAccessionNumber("ACC67890");

        benchmarkItemWriter.write(Chunk.of(first, second));

        verify(analyticsRepository).saveAllBenchmarks(List.of(first, second));
    }

}
