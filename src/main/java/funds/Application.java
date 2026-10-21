package funds;

import funds.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Map;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Active ONLY when the 'cli' profile is enabled (e.g., SPRING_PROFILES_ACTIVE=prod,cli).
     * Excluded in daemon/worker mode so the application remains alive listening to RabbitMQ.
     */
    @Bean
    @Profile("cli")
    public CommandLineRunner manualBatchRunner(Map<String, JobService> jobServices, Environment environment) {
        return args -> {
            String year = environment.getProperty("YEAR");
            String quarter = environment.getProperty("QUARTER");

            log.info("Starting MANUAL_BATCH execution via 'cli' profile for Year={}, Quarter={}", year, quarter);

            JobService service = jobServices.get("fundImportService");
            if (service == null) {
                throw new IllegalStateException("Could not find bean 'fundImportService'");
            }

            JobExecution execution = service.processJob();
            if (!ExitStatus.COMPLETED.getExitCode().equals(execution.getExitStatus().getExitCode())) {
                throw new IllegalStateException("Job execution failed: " + execution.getExitStatus());
            }

            log.info("MANUAL_BATCH execution finished successfully with status: {}", execution.getExitStatus());
        };
    }
}