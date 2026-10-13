package es.unizar.ghostmark.backend.cuenta.services;

import es.unizar.ghostmark.backend.cuenta.Cuenta;
import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.sesion.DatosNoValidosException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import es.unizar.ghostmark.backend.cuenta.dto.Aceptaciones;

@Service
public class ServicioCuenta {

    private final CuentaRepository cuentas;
    private final Clock reloj;

    public ServicioCuenta(CuentaRepository cuentas, Clock reloj) {
        this.cuentas = cuentas;
        this.reloj = reloj;
    }

    @Transactional
    public Map<String, Object> aceptar(UUID cuentaId, Aceptaciones aceptaciones) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (aceptaciones == null || !Boolean.TRUE.equals(aceptaciones.ubicacion())) {
        errores.put("ubicacion", "Debe aceptarse el permiso de ubicación");
        }

        if (aceptaciones == null
            || !Boolean.TRUE.equals(aceptaciones.avisoDisponibilidad())) {
        errores.put("avisoDisponibilidad", "Debe aceptarse el aviso de disponibilidad");
        }

        if (!errores.isEmpty()) {
        throw new DatosNoValidosException(errores);
        }

        Cuenta cuenta = cuentas.findById(cuentaId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "La cuenta no está disponible"));

        Instant ahora = Instant.now(reloj);

        cuenta.aceptarUbicacion(ahora);
        cuenta.aceptarAvisoDisponibilidad(ahora);

        return Map.of(
            "id", cuenta.getId().toString(),
            "alias", cuenta.getAlias(),
            "email", cuenta.getEmail(),
            "ubicacionAceptada", cuenta.getUbicacionAceptadaEn() != null,
            "avisoDisponibilidadAceptado",
                cuenta.getAvisoDisponibilidadAceptadoEn() != null,
            "creadaEn", cuenta.getCreadaEn().toString());
    }

    @Transactional
    public void borrar(UUID cuentaId) {
        Cuenta cuenta = cuentas.findById(cuentaId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "La cuenta no está disponible"));

        cuentas.delete(cuenta);
    }
}