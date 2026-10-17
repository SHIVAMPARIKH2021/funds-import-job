package funds.listener;

import funds.dto.EtlCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FundImportEventListener {

    private final JobLauncher jobLauncher;
    private final Job fundImportJob;

    public FundImportEventListener(JobLauncher jobLauncher, Job fundImportJob) {
        this.jobLauncher = jobLauncher;
        this.fundImportJob = fundImportJob;
    }

    @RabbitListener(queues = "fund.import.trigger.queue")
    public void onEtlCompleted(EtlCompletedEvent event) throws Exception {
        log.info("Received event: {} | Correlation: {} | Target: Year {} Q{}",
                event.eventId(), event.correlationId(), event.payload().year(), event.payload().quarter());

        var params = new JobParametersBuilder()
                .addLong("year", (long) event.payload().year())
                .addLong("quarter", (long) event.payload().quarter())
                .addString("force", String.valueOf(event.payload().force()))
                .addString("correlationId", event.correlationId())
                .addString("eventId", event.eventId())
                .addLong("timestamp", System.currentTimeMillis()) // Assures unique execution instance
                .toJobParameters();

        jobLauncher.run(fundImportJob, params);
        log.info("Spring Batch job completed successfully for event ID: {}", event.eventId());
    }
}