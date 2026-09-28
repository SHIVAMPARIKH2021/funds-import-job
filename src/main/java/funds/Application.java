package funds;

import funds.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    private final Map<String, JobService> jobServices;

    private final String dbUrl;

    public Application(@Value("${spring.datasource.url}") String dbUrl,
                       Map<String, JobService> jobServices) {
        this.jobServices = jobServices;
        this.dbUrl = dbUrl;
    }

	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
	}

    @Override
     public void run(String... args) throws Exception {
        System.out.println("Argument: " + "spring.profiles.active");
        System.out.println("Available JobService beans: " + jobServices.keySet());

        JobService service = jobServices.get("fundImportService");
        if (service == null) {
            throw new IllegalStateException(
                    "Could not find bean 'fundImportService'. Available beans: " + jobServices.keySet()
            );
        }

        service.processJob();
    }

}
