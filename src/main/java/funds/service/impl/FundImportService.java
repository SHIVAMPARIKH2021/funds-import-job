package funds.service.impl;

import funds.service.JobService;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("fundImportService")
public class FundImportService implements JobService {

    @Autowired
    private Job FundsImportJob;

    @Override
    public void processJob() {
        System.out.println("Importing raw funds...");
    }
}
