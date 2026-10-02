package funds.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FundImportPayload {

    private final FundMaster fundMaster;
    private final List<FundBenchmarkAssociation> associations;
}