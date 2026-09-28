package funds.repository;

import funds.entity.FundMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FundMasterRepository extends JpaRepository<FundMaster, Long> {

    Optional<FundMaster> findBySeriesId(String seriesId);

    Optional<FundMaster> findByPrimaryTicker(String primaryTicker);
}
