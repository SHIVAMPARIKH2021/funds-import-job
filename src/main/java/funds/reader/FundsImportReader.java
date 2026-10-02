package funds.reader;

import funds.constants.Tags;
import funds.model.RawFundSource;
import funds.repository.AnalyticsRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
@StepScope
public class FundsImportReader implements ItemReader<RawFundSource> {

    @Autowired
    private final AnalyticsRepository analyticsRepository;

    @Value("#{jobParameters['year']}")
    private String year;

    @Value("#{jobParameters['quarter']}")
    private String quarter;

    private Iterator<RawFundSource> fundIterator;

    public FundsImportReader(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public RawFundSource read() throws Exception {
        if(fundIterator == null) {
            // Fetch the fund data based on version and tag
            List<RawFundSource> funds = analyticsRepository.findRawFundsForQuarter(
                    Tags.STRATEGY_NARRATIVE_TEXT_BLOCK.getTag(),
                    calculateQuarterDateRange(Integer.parseInt(year),
                            Integer.parseInt(quarter)).get("minDate"),
                    calculateQuarterDateRange(Integer.parseInt(year),
                            Integer.parseInt(quarter)).get("maxDate"))
                    .stream().distinct().toList();
            this.fundIterator = funds.iterator();
        }
        return this.fundIterator.hasNext() ? this.fundIterator.next() : null;
    }

    public Map<String, LocalDate> calculateQuarterDateRange(int year, int quarter) {
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDate minDate = LocalDate.of(year, startMonth, 1);
        LocalDate maxDate = YearMonth.of(year, startMonth + 2).atEndOfMonth();

        return Map.of("minDate", minDate, "maxDate", maxDate);
    }
}
