package funds.listener;

import ch.qos.logback.core.util.StringUtil;
import funds.configuration.RabbitMqConfig;
import funds.dto.EtlCompletedEvent;
import funds.exception.NonRetryableContractException;
import funds.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FundImportEventListener {

    private final JobService fundImportService;

    public FundImportEventListener(@Qualifier("fundImportService") JobService fundImportService) {
        this.fundImportService = fundImportService;
    }

    @RabbitListener(
            queues = RabbitMqConfig.IMPORT_TRIGGER_QUEUE,
            containerFactory = "importJobListenerContainerFactory"
    )
    public void onFundImportTrigger(EtlCompletedEvent event) throws Exception {
        log.info("Received fund import trigger event: jobId={}, payload={}", event.jobId(), event.payload());

        // Contract validation: Fail fast without retry if required fields are missing
        if (event.jobId() == null || event.jobId().isBlank()) {
            throw new NonRetryableContractException("Payload validation failed: 'jobId' cannot be blank");
        }
        if (event.payload() == null || StringUtil.isNullOrEmpty(event.payload().quarter()) || StringUtil.isNullOrEmpty(event.payload().year())) {
            throw new NonRetryableContractException("Unsupported dataset payload: " + event.payload());
        }

        // Delegate to service layer
        JobExecution execution = fundImportService.processJob(event);

        if (execution.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException("Batch job failed or stopped with status: " + execution.getStatus());
        }

        log.info("Spring Batch execution finished successfully for jobId={}", event.jobId());
    }
}