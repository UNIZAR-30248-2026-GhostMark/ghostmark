package es.unizar.ghostmark.backend.sesion;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ControladorSesion {

    private final ServicioSesion sesiones;
    private final Clock reloj;

    public ControladorSesion(ServicioSesion sesiones, Clock reloj) {
        this.sesiones = sesiones;
        this.reloj = reloj;
    }

    @PostMapping("/cuentas")
    public ResponseEntity<Map<String, Object>> crearCuenta(
            @RequestBody Map<String, String> cuerpo) {
        Map<String, Object> sesion =
                sesiones.registrar(
                        cuerpo.get("alias"), cuerpo.get("email"), cuerpo.get("contrasena"),
                        Instant.now(reloj));
        return ResponseEntity.status(HttpStatus.CREATED).body(sesion);
    }

    @PostMapping("/sesiones")
    public Map<String, Object> iniciarSesion(@RequestBody Map<String, String> cuerpo) {
        return sesiones.iniciarSesion(
                cuerpo.get("email"), cuerpo.get("contrasena"), Instant.now(reloj));
    }

    @PostMapping("/sesiones/refresco")
    public Map<String, Object> refrescar(@RequestBody Map<String, String> cuerpo) {
        return sesiones.refrescar(cuerpo.get("tokenRefresco"), Instant.now(reloj));
    }

    @PostMapping("/sesiones/cierre")
    public ResponseEntity<Void> cerrar(@RequestBody Map<String, String> cuerpo) {
        sesiones.cerrarSesion(cuerpo.get("tokenRefresco"), Instant.now(reloj));
        return ResponseEntity.noContent().build();
    }
}
