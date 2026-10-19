package funds;

import funds.service.JobService;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.util.Map;

@SpringBootApplication
public class Application implements CommandLineRunner {

    private final Map<String, JobService> jobServices;
    private final Environment environment;

    @Autowired
    public Application(Map<String, JobService> jobServices, Environment environment) {
        this.jobServices = jobServices;
        this.environment = environment;
    }

    public static void main(String[] args) {
        // Run as a persistent daemon/service
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Only run via CLI if year/quarter were passed explicitly via terminal or env
        if (environment.containsProperty("year") && environment.containsProperty("quarter")) {
            JobService service = jobServices.get("fundImportService");
            if (service == null) {
                throw new IllegalStateException("Could not find bean 'fundImportService'");
            }

            JobExecution execution = service.processJob();
            if (!ExitStatus.COMPLETED.getExitCode().equals(execution.getExitStatus().getExitCode())) {
                throw new IllegalStateException("Job execution failed: " + execution.getExitStatus());
            }
        }
        // Otherwise, stay running and listen for RabbitMQ events!
    }
}