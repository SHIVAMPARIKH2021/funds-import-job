package funds.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class FundImportPayloadTest {

    @Test
    void testAllArgsConstructor_GettersReturnSameInstances() {
        FundMaster fundMaster = new FundMaster();
        fundMaster.setSeriesId("TEST123");
        List<FundBenchmarkAssociation> associations = Collections.singletonList(
                new FundBenchmarkAssociation("TEST123", 1, "Benchmark1", true, "ACC12345"));

        FundImportPayload payload = new FundImportPayload(fundMaster, associations);

        assertSame(fundMaster, payload.getFundMaster());
        assertSame(associations, payload.getAssociations());
    }
}
