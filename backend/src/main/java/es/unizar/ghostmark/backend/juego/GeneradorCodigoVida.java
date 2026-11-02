package es.unizar.ghostmark.backend.juego;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class GeneradorCodigoVida {

    private static final long PASO_SEGUNDOS = 60;

    public CodigoVida generar(byte[] secreto, Instant ahora) {
        long paso = Math.floorDiv(ahora.getEpochSecond(), PASO_SEGUNDOS);
        try {
            Mac hmac = Mac.getInstance("HmacSHA1");
            hmac.init(new SecretKeySpec(secreto, "HmacSHA1"));
            byte[] resumen = hmac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(paso).array());
            // Truncado dinámico de RFC 4226, usado por TOTP en RFC 6238
            int desplazamiento = resumen[resumen.length - 1] & 0x0f;
            int valor = ByteBuffer.wrap(resumen, desplazamiento, Integer.BYTES).getInt()
                    & 0x7fffffff;
            return new CodigoVida(String.format(Locale.ROOT, "%06d", valor % 1_000_000),
                    Instant.ofEpochSecond((paso + 1) * PASO_SEGUNDOS));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("No se puede calcular el código de vida", error);
        }
    }
}
