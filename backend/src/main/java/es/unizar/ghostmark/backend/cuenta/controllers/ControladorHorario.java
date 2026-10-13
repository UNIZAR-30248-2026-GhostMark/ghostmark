package es.unizar.ghostmark.backend.cuenta.controllers;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import es.unizar.ghostmark.backend.cuenta.services.ServicioHorario;
import es.unizar.ghostmark.backend.cuenta.dto.Horario;

@RestController
@RequestMapping("/api/v1/cuentas/yo/horario")
public class ControladorHorario {

  private final ServicioHorario servicio;

  public ControladorHorario(ServicioHorario servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  public Horario obtener(Authentication autenticacion) {
    return servicio.obtener(obtenerCuentaId(autenticacion));
  }

  @PutMapping
  public Horario guardar(
      Authentication autenticacion,
      @RequestBody Horario horario) {
    return servicio.guardar(obtenerCuentaId(autenticacion), horario);
  }

  private UUID obtenerCuentaId(Authentication autenticacion) {
    if (autenticacion == null || !autenticacion.isAuthenticated()) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "No autenticado");
    }

    try {
      return UUID.fromString(autenticacion.getName());
    } catch (IllegalArgumentException error) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Token de acceso no válido");
    }
  }
}