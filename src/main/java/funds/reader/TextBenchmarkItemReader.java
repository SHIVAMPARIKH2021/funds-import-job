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
public class TextBenchmarkItemReader implements ItemReader<RawBenchmarkSource> {

    private final AnalyticsRepository analyticsRepository;
    private Iterator<RawBenchmarkSource> iterator;
    private static final List<String> BENCHMARK_TAGS = List.of(
            Tags.MARKET_INDEX_PERFORMANCE_COLUMN_HEADING.getTag(),
            Tags.INDEX_NO_DEDUCTION_FOR_FEES_EXPENSES_TAXES.getTag(),
            Tags.PERFORMANCE_TABLE_NARRATIVE.getTag()
    );

    public TextBenchmarkItemReader(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public RawBenchmarkSource read() {

        if (iterator == null) {
            List<RawBenchmarkSource> unmapped = analyticsRepository.findUnresolvedBenchmarksFromTextFacts(BENCHMARK_TAGS);
            this.iterator = unmapped != null ? unmapped.iterator() : List.<RawBenchmarkSource>of().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}