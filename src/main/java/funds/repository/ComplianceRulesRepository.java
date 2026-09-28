package funds.repository;

import funds.entity.ComplianceRules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplianceRulesRepository extends JpaRepository<ComplianceRules, Integer> {

    List<ComplianceRules> findByIsActiveTrueOrderByPriorityAsc();

}