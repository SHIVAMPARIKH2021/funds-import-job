package funds.reader;

import funds.constants.Tags;
import funds.model.RawBenchmarkSource;
import funds.repository.AnalyticsRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
@StepScope
public class BenchmarkItemReader implements ItemReader<RawBenchmarkSource> {

    private final AnalyticsRepository analyticsRepository;
    private Iterator<RawBenchmarkSource> benchmarkIterator;

    private static final List<String> INCLUDED_TAGS = List.of(
            Tags.AVERAGE_ANNUAL_RETURN_ONE_YEAR.getTag(),
            Tags.AVERAGE_ANNUAL_RETURN_THREE_YEAR.getTag(),
            Tags.AVERAGE_ANNUAL_RETURN_FIVE_YEAR.getTag(),
            Tags.AVERAGE_ANNUAL_RETURN_TEN_YEAR.getTag(),
            Tags.AVERAGE_ANNUAL_RETURN_FIFTEEN_YEAR.getTag(),
            Tags.AVERAGE_ANNUAL_RETURN_TWENTY_FIVE_YEAR.getTag(),
            Tags.AVERAGE_ANNUAL_RETURN_INCEPTION.getTag(),
            Tags.AVERAGE_ANNUAL_RETURN_INCEPTION_ONE.getTag(),
            Tags.AVERAGE_ANNUAL_RETURN_INCEPTION_TWO.getTag()
    );

    private static final List<String> EXCLUDED_TAGS = List.of(
            Tags.RETURN_BEFORE_TAXES.getTag(),
            Tags.AFTER_TAXES_ON_DISTRIBUTIONS.getTag(),
            Tags.AFTER_TAXES_ON_DISTRIBUTIONS_AND_SALES.getTag()
    );

    public BenchmarkItemReader(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public RawBenchmarkSource read() {
        if (benchmarkIterator == null) {
            List<RawBenchmarkSource> benchmarks = analyticsRepository.findAllBenchmarksByTags(
                    INCLUDED_TAGS,
                    EXCLUDED_TAGS
            );
            this.benchmarkIterator = benchmarks != null ? benchmarks.iterator() : List.<RawBenchmarkSource>of().iterator();
        }

        // Returns one item at a time; returns null when exhausted to signal step completion
        return benchmarkIterator.hasNext() ? benchmarkIterator.next() : null;
    }
}