package funds.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EtlCompletedEvent(
        String specVersion,
        String jobId,
        String eventType,
        Instant timestamp,
        String source,
        String correlationId,
        FundPayload payload
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FundPayload(
            String year,
            String quarter,
            boolean isTicker,
            boolean isForcedImport,
            Long recordsProcessed,
            String status
    ) {}
}