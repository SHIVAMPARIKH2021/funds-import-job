package funds.reader;

import funds.model.RawFundSource;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

public class FundsImportReader implements ItemReader {

    @Autowired
    private ComplianceRulesRepository complianceRulesRepository;

    @Autowired

    @Override
    public RawFundSource read() throws Exception {

        return null;
    }
}
