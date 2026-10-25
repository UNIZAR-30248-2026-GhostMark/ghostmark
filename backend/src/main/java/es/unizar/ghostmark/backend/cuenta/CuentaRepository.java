package es.unizar.ghostmark.backend.cuenta;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.id = :id")
    Optional<Cuenta> bloquearPorId(UUID id);

    Optional<Cuenta> findByEmail(String email);

    boolean existsByAliasIgnoreCase(String alias);

    boolean existsByEmailIgnoreCase(String email);
}
