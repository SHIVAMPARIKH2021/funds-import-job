package funds.service.impl;

import ch.qos.logback.core.util.StringUtil;
import funds.exception.InvalidParameterException;
import funds.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("fundImportService")
public class FundImportService implements JobService {

    private final JobLauncher jobLauncher;
    private final Job fundsImportJob;
    private final JobExplorer jobExplorer;

    @Value("${quarter:#{null}}")
    private String quarter;

    @Value("${year:#{null}}")
    private String year;

    @Value("${executor:System}")
    private String executor;

    @Value("${dryrun:default}")
    private String dryRun;

    @Value("${force:false}")
    private boolean force;

    @Autowired
    public FundImportService(JobLauncher jobLauncher, Job fundsImportJob, JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.fundsImportJob = fundsImportJob;
        this.jobExplorer = jobExplorer;
    }

    @Override
    public JobExecution processJob() throws InvalidParameterException, JobInstanceAlreadyCompleteException,
            JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {

        if (StringUtil.isNullOrEmpty(quarter)) {
            throw new InvalidParameterException("[" + quarter + "] quarter is invalid.");
        }
        if (StringUtil.isNullOrEmpty(year)) {
            throw new InvalidParameterException("[" + year + "] year is invalid.");
        }

        String targetYear = year.trim();
        String targetQuarter = quarter.trim();

        if (!force && isQuarterAlreadyCompleted(targetYear, targetQuarter)) {
            String errorMsg = String.format(
                    "Job execution aborted: A successful execution already exists for Year '%s' and Quarter '%s'. Set 'force=true' to override.",
                    targetYear, targetQuarter
            );
            log.warn(errorMsg);
            throw new JobInstanceAlreadyCompleteException(errorMsg);
        }

        String resolvedExecutor = (executor != null && !executor.isBlank()) ? executor.trim() : "System";

        JobParameters params = new JobParametersBuilder()
                .addString("executor", resolvedExecutor)
                .addString("quarter", targetQuarter)
                .addString("year", targetYear)
                .addLong("runId", System.currentTimeMillis())
                .addString("dryrun", dryRun)
                .toJobParameters();

        return jobLauncher.run(fundsImportJob, params);
    }

    private boolean isQuarterAlreadyCompleted(String targetYear, String targetQuarter) {
        String jobName = fundsImportJob.getName();
        try {
            int instanceCount = Math.toIntExact(jobExplorer.getJobInstanceCount(jobName));
            if (instanceCount == 0) {
                return false;
            }

            List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, instanceCount);

            for (JobInstance instance : jobInstances) {
                List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
                for (JobExecution execution : executions) {
                    if (execution.getStatus() == BatchStatus.COMPLETED) {
                        JobParameters params = execution.getJobParameters();
                        String execYear = params.getString("year");
                        String execQuarter = params.getString("quarter");

                        if (targetYear.equals(execYear) && targetQuarter.equals(execQuarter)) {
                            log.info("Found completed run (JobExecutionId: {}) for Year: {}, Quarter: {}",
                                    execution.getId(), execYear, execQuarter);
                            return true;
                        }
                    }
                }
            }
        } catch (NoSuchJobException e) {
            log.info("No prior job instances exist for '{}'. Proceeding with execution.", jobName);
            return false;
        }
        return false;
    }
}