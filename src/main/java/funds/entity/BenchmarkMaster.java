package funds.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(schema = "analytics", name = "benchmark_master")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class BenchmarkMaster extends BaseAuditableEntity {

    @Id
    @Column(name = "benchmark_id", length = 64)
    private String benchmarkId;

    @Column(name = "benchmark_name", nullable = false, unique = true, columnDefinition = "TEXT")
    private String benchmarkName;

    @Column(name = "benchmark_provider", length = 64)
    private String benchmarkProvider;

    @Column(name = "benchmark_type", length = 32)
    private String benchmarkType = "Broad Market";

    public BenchmarkMaster(String benchmarkId, String benchmarkName, String benchmarkProvider) {
        this.benchmarkId = benchmarkId;
        this.benchmarkName = benchmarkName;
        this.benchmarkProvider = benchmarkProvider;
    }
}