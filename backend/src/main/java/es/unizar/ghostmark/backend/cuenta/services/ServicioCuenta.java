package es.unizar.ghostmark.backend.cuenta.services;

import es.unizar.ghostmark.backend.cuenta.Cuenta;
import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.cuenta.dto.Aceptaciones;
import es.unizar.ghostmark.backend.inscripcion.ServicioInscripcion;
import es.unizar.ghostmark.backend.partida.ServicioPartida;
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

@Service
public class ServicioCuenta {

    private final CuentaRepository cuentas;
    private final Clock reloj;
    private final ServicioPartida partidas;

    private final ServicioInscripcion inscripciones;

    public ServicioCuenta(CuentaRepository cuentas, Clock reloj, ServicioPartida partidas,
            ServicioInscripcion inscripciones) {
        this.cuentas = cuentas;
        this.reloj = reloj;
        this.partidas = partidas;
        this.inscripciones = inscripciones;
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
        final Cuenta cuenta = cuentas.bloquearPorId(cuentaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "La cuenta no está disponible"));

        Instant ahora = Instant.now(reloj);
        inscripciones.bloquearPartidasDeCuenta(cuentaId);
        partidas.cancelarPorBorradoDeMaster(cuentaId, ahora);
        inscripciones.borrarPorCuenta(cuentaId, ahora);
        cuentas.delete(cuenta);
    }
}
