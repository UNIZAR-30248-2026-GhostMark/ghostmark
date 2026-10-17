package es.unizar.ghostmark.backend.health;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthTest {
    @Test void healthUp() {
        assertEquals("UP", new HealthController().health().get("status"));
    }
}