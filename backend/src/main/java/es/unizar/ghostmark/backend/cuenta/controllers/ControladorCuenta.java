package es.unizar.ghostmark.backend.cuenta.controllers;

import es.unizar.ghostmark.backend.cuenta.dto.Aceptaciones;
import es.unizar.ghostmark.backend.cuenta.services.ServicioCuenta;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/cuentas/yo")
public class ControladorCuenta {

    private final ServicioCuenta servicio;

    public ControladorCuenta(ServicioCuenta servicio) {
        this.servicio = servicio;
    }

    @PutMapping("/aceptaciones")
    public Map<String, Object> aceptar(
            Authentication autenticacion,
            @RequestBody Aceptaciones aceptaciones) {

        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        try {
            UUID cuentaId = UUID.fromString(autenticacion.getName());
            return servicio.aceptar(cuentaId, aceptaciones);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token de acceso no válido");
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        UUID cuentaId;
        try {
            cuentaId = UUID.fromString(autenticacion.getName());
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token de acceso no válido");
        }

        servicio.borrar(cuentaId);
    }
}