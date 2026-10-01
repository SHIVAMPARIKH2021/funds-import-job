package funds.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseModel {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
