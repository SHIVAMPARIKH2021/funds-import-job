package funds.processor;

import funds.entity.FundMaster;
import funds.model.RawFundSource;
import org.springframework.batch.item.ItemProcessor;

public class FundsItemProcessor implements ItemProcessor<RawFundSource, FundMaster> {

    @Override
    public FundMaster process(RawFundSource item) throws Exception {
        // Implement your processing logic here
        return null;
    }

}
