package es.unizar.ghostmark.backend.sesion;

import es.unizar.ghostmark.backend.cuenta.Cuenta;
import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.cuenta.TokenRefresco;
import es.unizar.ghostmark.backend.cuenta.TokenRefrescoRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioSesion {

    private static final String PATRON_EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    static final Duration ACCESO_DURACION = Duration.ofMinutes(15);
    static final Duration REFRESCO_DURACION = Duration.ofDays(30);

    private final CuentaRepository cuentas;
    private final TokenRefrescoRepository refrescos;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    private final SecureRandom aleatorio = new SecureRandom();
    private final SecretKey firma;

    public ServicioSesion(
            CuentaRepository cuentas,
            TokenRefrescoRepository refrescos,
            @Value("${JWT_SECRETO:}") String secreto) {
        this.cuentas = cuentas;
        this.refrescos = refrescos;
        if (secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Falta JWT_SECRETO de al menos 32 bytes en el entorno");
        }
        this.firma = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public Map<String, Object> registrar(String alias, String email, String contrasena,
                                         Instant ahora) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (alias == null || !alias.matches("^[A-Za-z0-9_]{3,20}$")) {
            errores.put("alias", "De 3 a 20: letras sin tildes, números y guion bajo");
        }
        validarEmail(email, errores);
        validarContrasena(contrasena, errores);
        if (!errores.isEmpty()) {
            throw new DatosNoValidosException(errores);
        }
        if (cuentas.existsByAliasIgnoreCase(alias)) {
            throw new CuentaExisteException("ALIAS_OCUPADO", "Ese alias ya está en uso");
        }
        if (cuentas.existsByEmailIgnoreCase(email)) {
            throw new CuentaExisteException("EMAIL_OCUPADO", "Ese email ya está en uso");
        }
        Cuenta cuenta =
                new Cuenta(alias, email, bcrypt.encode(contrasena), ahora);
        cuentas.save(cuenta);
        return sesion(cuenta, ahora);
    }

    @Transactional
    public Map<String, Object> iniciarSesion(String email, String contrasena, Instant ahora) {
        Map<String, String> errores = new LinkedHashMap<>();
        validarEmail(email, errores);
        validarContrasena(contrasena, errores);
        if (!errores.isEmpty()) {
            throw new DatosNoValidosException(errores);
        }
        Cuenta cuenta =
                cuentas
                        .findByEmail(email == null ? null : email.toLowerCase())
                        .filter(c -> contrasena != null && bcrypt.matches(contrasena,
                                c.getHashContrasena()))
                        .orElseThrow(
                                () -> new CredencialesException("Email o contraseña incorrectos"));
        return sesion(cuenta, ahora);
    }

    private static void validarEmail(String email, Map<String, String> errores) {
        if (email == null || email.length() > 254 || !email.matches(PATRON_EMAIL)) {
            errores.put("email", "Debe ser un email válido");
        }
    }

    private static void validarContrasena(String contrasena, Map<String, String> errores) {
        if (contrasena == null || contrasena.length() < 8 || contrasena.length() > 72) {
            errores.put("contrasena", "Debe tener entre 8 y 72 caracteres");
        }
    }

    @Transactional
    public Map<String, Object> refrescar(String token, Instant ahora) {
        TokenRefresco anterior =
                refrescos
                        .findByHashToken(sha256(token))
                        .filter(t -> t.valePara(ahora))
                        .orElseThrow(() -> new TokenException("El refresco no vale"));
        anterior.revocar(ahora);
        return sesion(anterior.getCuenta(), ahora);
    }

    @Transactional
    public void cerrarSesion(String token, Instant ahora) {
        refrescos.findByHashToken(sha256(token)).ifPresent(t -> t.revocar(ahora));
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void borrarRefrescosCaducados() {
        refrescos.deleteByCaducaEnBefore(Instant.now().minus(REFRESCO_DURACION));
    }

    private Map<String, Object> sesion(Cuenta cuenta, Instant ahora) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String refresco = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refrescos.save(
                new TokenRefresco(cuenta, sha256(refresco), ahora.plus(REFRESCO_DURACION), ahora));
        String acceso =
                Jwts.builder()
                        .subject(cuenta.getId().toString())
                        .issuedAt(java.util.Date.from(ahora))
                        .expiration(java.util.Date.from(ahora.plus(ACCESO_DURACION)))
                        .signWith(firma)
                        .compact();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("tokenAcceso", acceso);
        respuesta.put("tokenRefresco", refresco);
        respuesta.put("accesoCaducaEn", ahora.plus(ACCESO_DURACION).toString());
        Map<String, Object> cuentaJson = new LinkedHashMap<>();
        cuentaJson.put("id", cuenta.getId().toString());
        cuentaJson.put("alias", cuenta.getAlias());
        cuentaJson.put("email", cuenta.getEmail());
        cuentaJson.put("ubicacionAceptada", cuenta.getUbicacionAceptadaEn() != null);
        cuentaJson.put("avisoDisponibilidadAceptado",
                cuenta.getAvisoDisponibilidadAceptadoEn() != null);
        cuentaJson.put("creadaEn", cuenta.getCreadaEn().toString());
        respuesta.put("cuenta", cuentaJson);
        return respuesta;
    }

    static String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException imposible) {
            throw new IllegalStateException(imposible);
        }
    }
}
