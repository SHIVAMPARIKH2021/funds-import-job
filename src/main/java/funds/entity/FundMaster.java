package funds.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Entity
@Table(schema = "analytics", name = "fund_master")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "benchmark")
public class FundMaster extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fund_id")
    private Long fundId;

    @Column(name = "series_id", nullable = false, unique = true, length = 10)
    private String seriesId;

    @Column(name = "cik", nullable = false)
    private Integer cik;

    @Column(name = "fund_name", nullable = false, columnDefinition = "TEXT")
    private String fundName;

    @Column(name = "fund_family", columnDefinition = "TEXT")
    private String fundFamily;

    @Column(name = "primary_ticker", length = 10)
    private String primaryTicker;

    @Column(name = "investment_objective", columnDefinition = "TEXT")
    private String investmentObjective;

    @Column(name = "strategy_narrative", columnDefinition = "TEXT")
    private String strategyNarrative;

    @Column(name = "strategy_type", nullable = false, length = 20)
    private String strategyType; // 'ACTIVE', 'PASSIVE', 'HYBRID', 'UNKNOWN'

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "benchmark_id")
    private BenchmarkMaster benchmark;

    @Column(name = "benchmark_name", columnDefinition = "TEXT")
    private String benchmarkName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FundMaster that)) return false;
        return seriesId != null && Objects.equals(seriesId, that.seriesId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}