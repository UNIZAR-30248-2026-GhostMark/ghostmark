package es.unizar.ghostmark.backend.cuenta;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRefrescoRepository extends JpaRepository<TokenRefresco, UUID> {

  Optional<TokenRefresco> findByHashToken(String hashToken);

  long deleteByCaducaEnBefore(Instant limite);
}
