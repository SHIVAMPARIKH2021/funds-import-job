package funds.tasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CleanStagedFilingBenchmarkTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;
    private final String truncateSql;

    public CleanStagedFilingBenchmarkTasklet(
            JdbcTemplate jdbcTemplate,
            @Qualifier("cleanStagedFilingBenchmarkSql") String truncateSql
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.truncateSql = truncateSql;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int deletedRows =  jdbcTemplate.update(this.truncateSql);
        log.info("Deleted staged records: {}", deletedRows);
        return RepeatStatus.FINISHED;
    }
}