package in.artisanconnect.repo;
import in.artisanconnect.domain.EarningsLedger;import org.springframework.data.jpa.repository.JpaRepository;import java.util.List;
public interface EarningsLedgerRepository extends JpaRepository<EarningsLedger,Long>{List<EarningsLedger> findByArtisanIdOrderByRecordedAtDesc(Long id);}
