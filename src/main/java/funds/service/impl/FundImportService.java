package funds.service.impl;

import ch.qos.logback.core.util.StringUtil;
import funds.exception.InvalidParameterException;
import funds.service.JobService;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("fundImportService")
public class FundImportService implements JobService {

    private final JobLauncher jobLauncher;

    @Autowired
    private Job fundsImportJob;

    @Value("quarter")
    private String quarter;

    @Value("year")
    private String year;

    @Value("executor")
    private String executor;

    @Value("series")
    private String seriesId;

    @Value("refresh")
    private String forceFullRefresh;

    @Value("dryrun")
    private String dryRun;

    public FundImportService(JobLauncher jobLauncher,
                             Job fundsImportJob,
                             String quarter,
                             String year,
                             String executor,
                             String seriesId,
                             String forceFullRefresh,
                             String dryRun
                             ) {
        this.jobLauncher = jobLauncher;
        this.fundsImportJob = fundsImportJob;
        this.year = year;
        this.quarter = quarter;
        this.executor = executor;
        this.seriesId = seriesId;
        this.forceFullRefresh = forceFullRefresh;
        this.dryRun = dryRun;
    }

    @Override
    public JobExecution processJob() throws InvalidParameterException, JobInstanceAlreadyCompleteException,
            JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        if (StringUtil.isNullOrEmpty(quarter)) {
            throw new InvalidParameterException("[" + quarter +"]" + " quarter is invalid.");
        }
        if (StringUtil.isNullOrEmpty(year)) {
            throw new InvalidParameterException("[" + year +"]" + " quarter is invalid.");
        }
        JobParameters params = new JobParametersBuilder()
                .addString("executor", (executor != null && !executor.isBlank()) ? executor : "SYSTEM")
                .addString("quarter", quarter)
                .addString("year", year)
                .addLong("runId", System.currentTimeMillis())
                //Optional parameters
                .addString("seriesId", seriesId)
                .addString("forceFullRefresh", forceFullRefresh)
                .addString("dryRun", dryRun)
                .toJobParameters();

        return jobLauncher.run(fundsImportJob, params);
    }
}
