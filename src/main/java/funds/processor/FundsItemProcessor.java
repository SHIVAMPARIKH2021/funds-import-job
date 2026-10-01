package funds.processor;

import ch.qos.logback.core.util.StringUtil;
import funds.model.BenchmarkMaster;
import funds.model.ComplianceRules;
import funds.model.FundMaster;
import funds.model.RawFundSource;
import funds.repository.AnalyticsRepository;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class FundsItemProcessor implements ItemProcessor<RawFundSource, FundMaster> {

    @Value("{jobParameters['executor']}")
    private String executor;

    @Value("{jobParameters['dryrun']}")
    private String dryRun;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    private Map<Integer, Pattern> complianceRuleMap = new HashMap<>();

    private List<ComplianceRules> complianceRules;


    @BeforeStep
    public void setup() {
        complianceRules = analyticsRepository.findByIsActiveTrueOrderByPriorityAsc();
        complianceRules.forEach(rule -> {
            if (Boolean.TRUE.equals(rule.getIsRegex())) {
                complianceRuleMap.put(rule.getRuleId(),
                        Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
            }
        });
    }


    @Override
    public FundMaster process(RawFundSource item) throws Exception {
        if (StringUtil.isNullOrEmpty(item.getSeriesId())) {
            return null; // Filtering invalid input; skipped by writer
        }

        // 1. Resolve existing entity for Upsert, or instantiate a new one
        FundMaster fund = analyticsRepository.findBySeriesId(item.getSeriesId())
                .orElseGet(() -> {
                    FundMaster newFund = new FundMaster();
                    newFund.setSeriesId(item.getSeriesId());
                    newFund.setCreatedBy(this.executor); // Honors explicit executor audit tag
                    return newFund;
                });

        fund.setCik(item.getCik());
        fund.setPrimaryTicker(item.getTicker());
        fund.setStrategyNarrative(item.getStrategyNarrative());

        // 2. Classify Strategy Type via Compliance Rules
        String classifiedStrategy = evaluateStrategy(item.getStrategyNarrative());
        fund.setStrategyType(classifiedStrategy);


    // Return null on dry runs to prevent persistence
        return Boolean.getBoolean(this.dryRun.toLowerCase()) ? null : fund;
}

private String evaluateStrategy(String narrative) {
    String UNKNOWN_FUND_TYPE = "UNKNOWN";
    if (StringUtil.isNullOrEmpty(narrative)) {
        return UNKNOWN_FUND_TYPE;
    }

    for (ComplianceRules rule : complianceRules) {
        boolean matches;
        if (Boolean.TRUE.equals(rule.getIsRegex())) {
            Pattern pattern = complianceRuleMap.get(rule.getRuleId());
            matches = pattern != null && pattern.matcher(narrative).find(); //Finds keyword 'Active'/'Passive'
        } else {
            matches = narrative.toLowerCase().contains(rule.getPattern().toLowerCase());
        }

        if (matches) {
            return rule.getTargetStrategy();
        }
    }

    return UNKNOWN_FUND_TYPE;
}

}
