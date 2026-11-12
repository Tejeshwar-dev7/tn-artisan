package in.artisanconnect.repo;
import in.artisanconnect.domain.ArtisanProfile; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ArtisanRepository extends JpaRepository<ArtisanProfile,Long>{ Optional<ArtisanProfile> findByUserId(Long id); List<ArtisanProfile> findByStatusOrderByIdDesc(ArtisanProfile.Verification status); }
