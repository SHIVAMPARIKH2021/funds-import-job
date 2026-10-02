package funds.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawFundSource extends BaseModel {
        // Identifiers & SEC Filing Metadata
        private String seriesId;
        private Integer cik;
        private String accessionNumber;
        private LocalDate filingDate;

        // Core Fund Profile & Metadata
        private String ticker;
        private String classId;
        private String fundFamily;

        // Narrative Blocks (Target of Regex & Compliance Classification)
        private String strategyNarrative;
        private String additonalStrategyNarrative;
        private String benchmarkName;

        // Audit / Coordination Watermarks
        private String filingQuarter;
        private Integer filingYear;
}