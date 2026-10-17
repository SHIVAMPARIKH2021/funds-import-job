package funds.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EtlCompletedEvent(
        String specVersion,
        String eventId,
        String eventType,
        Instant timestamp,
        String source,
        String correlationId,
        FundPayload payload
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FundPayload(
            int year,
            int quarter,
            String ticker,
            boolean force,
            int recordsProcessed,
            String status
    ) {}
}