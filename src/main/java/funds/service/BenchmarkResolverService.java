package funds.service;

import funds.model.BenchmarkMaster;
import funds.model.FundMaster;
import funds.model.RawFundSource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class BenchmarkResolverService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "of", "and", "in", "for", "to", "a", "an", "index", "total", "return", "fund", "series", "class", "trust"
    );

    private static final Pattern BENCHMARK_BOILERPLATE_PATTERN = Pattern.compile(
            "(?i)\\s*[\\(\\[\\*]?\\s*(?:reflects?\\s+no\\s+deductions?|no\\s+deductions?|reflects?\\s+no\\s+fees?|gross\\s+of\\s+taxes?|net\\s+of\\s+taxes?).*"
    );

    private static final Pattern CLEAN_PUNCTUATION_PATTERN = Pattern.compile(
            "[*,\\[\\]()./\\-—_\"'%&]"
    );

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * Scoring Tie-Breaker method for benchmark selection
     */
    public BenchmarkMaster selectPrimaryBenchmark(List<BenchmarkMaster> candidates, RawFundSource item, FundMaster fund, String fundName) {
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }

        // Normalize text from both the item and the populated fund entity
        String normObjective = normalizeText(fund.getInvestmentObjective());
        String normFundName = normalizeText(fundName);
        String normNarrative = normalizeText(item.getStrategyNarrative());

        BenchmarkMaster bestMatch = candidates.getFirst();
        int highestScore = -1;

        for (BenchmarkMaster bm : candidates) {
            String rawBmName = bm.getBenchmarkName();
            if (rawBmName == null || rawBmName.isBlank()) {
                continue;
            }

            // Strip disclaimer boilerplate
            String cleanBm = BENCHMARK_BOILERPLATE_PATTERN.matcher(rawBmName).replaceAll("");
            String normBm = normalizeText(cleanBm);

            if (normBm.length() < 3) {
                normBm = normalizeText(rawBmName);
            }

            int objectiveScore = scoreCandidateMatch(normBm, normObjective) * 3; // Weight x3
            int fundNameScore = scoreCandidateMatch(normBm, normFundName) * 2;   // Weight x2
            int narrativeScore = scoreCandidateMatch(normBm, normNarrative);     // Weight x1

            int totalScore = objectiveScore + fundNameScore + narrativeScore;

            if (totalScore > highestScore) {
                highestScore = totalScore;
                bestMatch = bm;
            }
        }

        return bestMatch;
    }

    private int scoreCandidateMatch(String normBm, String targetText) {
        if (normBm.isBlank() || targetText.isBlank()) {
            return 0;
        }

        // Tier 1: Exact full phrase match
        if (targetText.contains(normBm)) {
            return 100;
        }

        // Tier 2: Phrase match omitting the word "index"
        String withoutIndex = normBm.replaceAll("\\bindex\\b", "").replaceAll("\\s+", " ").trim();
        if (withoutIndex.length() >= 3 && targetText.contains(withoutIndex)) {
            return 85;
        }

        // Tier 3: Token overlap
        String[] tokens = normBm.split("\\s+");
        List<String> keyTokens = new ArrayList<>();
        for (String t : tokens) {
            if (!STOP_WORDS.contains(t) && t.length() > 1) {
                keyTokens.add(t);
            }
        }

        if (keyTokens.isEmpty()) {
            return 0;
        }

        int matchedCount = 0;
        boolean hasDistinctiveNumber = false;

        for (String token : keyTokens) {
            Pattern wordPattern = Pattern.compile("\\b" + Pattern.quote(token) + "\\b");
            if (wordPattern.matcher(targetText).find()) {
                matchedCount++;
                if (!hasDistinctiveNumber && token.matches("\\d+")) {
                    hasDistinctiveNumber = true;
                }
            }
        }

        double ratio = (double) matchedCount / keyTokens.size();
        if (ratio >= 0.75) {
            return (int) (60 * ratio) + (hasDistinctiveNumber ? 20 : 0);
        } else if (ratio >= 0.50) {
            return (int) (30 * ratio) + (hasDistinctiveNumber ? 15 : 0);
        }

        return 0;
    }

    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // Unescape &amp;, &quot;, &#160; etc. using Spring's built-in utility
        String unescaped = HtmlUtils.htmlUnescape(text.toLowerCase());
        unescaped = unescaped.replaceAll("<[^>]+>", " ");
        String stripped = CLEAN_PUNCTUATION_PATTERN.matcher(unescaped).replaceAll(" ");
        return WHITESPACE_PATTERN.matcher(stripped).replaceAll(" ").trim();
    }
}
