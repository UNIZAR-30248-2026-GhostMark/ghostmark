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

## Compartir con QR y enlace (#50) y escanear invitaciones (#56)

Después de crear una partida aparece el paso **Compartir** (`f2-e5`). El QR y la copia usan el `enlace` que devuelve el servidor (`ghostmark://unirse/<codigo>`). El botón **Ir a la partida** consulta la información pública del servidor de la tarea #48. Desde Inicio, **Unirse** abre el escáner (`f3-d1`): solicita el permiso de cámara al usarlo, acepta los QR con el enlace de GhostMark y abre la misma vista pública. También se puede abrir el enlace directamente desde Android. Ver la partida no crea una inscripción; las normas y la sala de espera corresponden a la tarea #57.

Para ejecutar las comprobaciones:

```powershell
npm ci
npm run typecheck
npm run lint
npm test
```

Las pruebas de la #50 verifican el flujo de creación, el enlace usado en el QR y al copiar, el contrato de consulta, el reintento, las respuestas tardías y la recepción de enlaces antes y después de iniciar sesión. Las APIs nativas y HTTP se simulan en estas pruebas.

Para probar el esquema `ghostmark://` y la cámara, instala una compilación nativa que incluya `scheme` y `expo-camera` (por ejemplo `npx expo run:android` con Android Studio/SDK y un emulador o dispositivo conectado). Si Expo solicita un identificador de aplicación, coordínalo con el responsable del despliegue. Cambiar `app.json` o reiniciar Metro no añade el módulo de cámara ni registra el esquema en un APK ya instalado; hace falta un binario nuevo.

Con Expo Go puedes comprobar las pantallas, el QR y copiar el enlace. Para probar la navegación por invitación usa `exp://<IP-del-ordenador>:8081/--/unirse/<codigo>` con la dirección real de Metro. El QR compartido sigue conteniendo la URL oficial `ghostmark://` y requiere una instalación nativa de GhostMark para abrirse.

En Android, con el binario instalado y Metro accesible, se puede abrir un código real desde PowerShell:

```powershell
adb shell am start -W -a android.intent.action.VIEW -d "ghostmark://unirse/CODIGO_REAL"
```

Sustituye `CODIGO_REAL` por el código de ocho caracteres devuelto al crear la partida. Comprueba la apertura con la app abierta, cerrada y sin sesión, además de un código válido que no exista (404). Escanea el QR real desde Inicio y comprueba el permiso aceptado, denegado y bloqueado. El móvil físico debe usar la IP LAN del ordenador en `EXPO_PUBLIC_API_URL`; el emulador Android usa `http://10.0.2.2:8080`.
