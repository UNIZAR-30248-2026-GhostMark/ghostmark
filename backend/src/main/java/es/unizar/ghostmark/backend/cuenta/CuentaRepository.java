package es.unizar.ghostmark.backend.cuenta;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {

    Optional<Cuenta> findByEmail(String email);

    boolean existsByAliasIgnoreCase(String alias);

    boolean existsByEmailIgnoreCase(String email);
}
