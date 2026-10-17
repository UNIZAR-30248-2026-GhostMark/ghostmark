# GhostMark Mobile

Esqueleto de la app de GhostMark para la tarea #35. Incluye Expo con TypeScript, la navegación inicial, el tema visual compartido y una comprobación del servidor.

## Arranque local

1. El Dockerfile del servidor necesita primero el JAR de Spring Boot. Desde la raíz del repositorio:

   ```powershell
   Push-Location backend
   .\mvnw.cmd clean verify
   Pop-Location
   docker compose up --build
   ```

2. Copia `.env.example` como `.env` dentro de `mobile/` y ajusta la URL si es necesario.

   - Navegador web: `http://localhost:8080`
   - Emulador Android: `http://10.0.2.2:8080`
   - Móvil físico: `http://<IP-LAN-DEL-ORDENADOR>:8080`

3. Instala y arranca Expo:

   ```powershell
   npm install
   npm start
   ```

En la pantalla inicial, «COMPROBAR SERVIDOR» llama a `GET /api/v1/health`. Una respuesta `UP` confirma la integración inicial con la API.
