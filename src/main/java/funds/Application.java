package funds;

import funds.service.JobService;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class Application implements CommandLineRunner {

    private final Map<String, JobService> jobServices;

    @Autowired
    public Application(@Value("${spring.datasource.url}") String dbUrl,
                       Map<String, JobService> jobServices) {
        this.jobServices = jobServices;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
    }

    @Override
    public void run(String... args) throws Exception {
        JobService service = jobServices.get("fundImportService");
        if (service == null) {
            throw new IllegalStateException(
                    "Could not find bean 'fundImportService'. Available beans: " + jobServices.keySet()
            );
        }

        JobExecution execution = service.processJob();
        if (!ExitStatus.COMPLETED.getExitCode().equals(execution.getExitStatus().getExitCode())) {
            throw new IllegalStateException("Spring Batch Job execution ended with status: "
                    + execution.getStatus() + " and exit code: " + execution.getExitStatus().getExitCode());
        }
    }
}