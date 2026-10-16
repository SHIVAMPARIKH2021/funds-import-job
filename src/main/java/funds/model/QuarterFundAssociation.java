package funds.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuarterFundAssociation {
    private int quarterId;
    private String seriesId;
    private String accessionNumber;
    private String createdBy;
}