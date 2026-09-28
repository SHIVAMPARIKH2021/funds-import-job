package funds.writer;

import funds.entity.FundMaster;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class FundsItemWriter implements ItemWriter<FundMaster> {

    @Override
    public void write(Chunk<? extends FundMaster> chunk) throws Exception {
        // Saves the batch of entities within the current chunk transaction
    }
}