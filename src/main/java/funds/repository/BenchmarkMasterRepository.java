package funds.repository;

import funds.entity.BenchmarkMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BenchmarkMasterRepository extends JpaRepository<BenchmarkMaster, String> {

    Optional<BenchmarkMaster> findByBenchmarkName(String benchmarkName);
}