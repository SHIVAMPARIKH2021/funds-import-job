package funds;

import funds.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    private final Map<String, JobService> jobServices;

    public Application(@Value("${spring.datasource.url}") String dbUrl,
                       Map<String, JobService> jobServices) {
        this.jobServices = jobServices;
    }

	public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        int exitCode = SpringApplication.exit(context);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
	}

    @Override
     public void run(String... args) throws Exception {

        JobService service = jobServices.get("fundImportService");
        if (service == null) {
            throw new IllegalStateException(
                    "Could not find bean 'fundImportService'. Available beans: " + jobServices.keySet()
            );
        }

        service.processJob();
    }

}
