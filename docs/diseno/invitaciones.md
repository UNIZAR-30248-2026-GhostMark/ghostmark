# Invitaciones: compartir y abrir una partida (#50)

El servidor devuelve `codigoInvitacion` y `enlace` (`ghostmark://unirse/<codigoInvitacion>`, con ocho caracteres del alfabeto `23456789ABCDEFGHJKMNPQRSTUVWXYZ`) al crear la partida, y el móvil usa ese `enlace` tal cual.

`CreateGameRules` crea la partida una sola vez y sustituye el historial del asistente por `Home → ShareGame`. El paso «Compartir» no vuelve a realizar el POST y volver atrás no permite reenviar el formulario ya completado. `ShareGame` presenta el paso 5 del prototipo `f2-e5`, un QR generado localmente, el enlace seleccionable y la copia al portapapeles (sin menú de compartir del móvil, decisión R9-5). El QR tiene margen blanco y la ilustración queda fuera de sus módulos.

`app.json` registra el esquema `ghostmark`. El navegador recibe tanto la URL inicial como los eventos de URL con la aplicación abierta. Las invitaciones válidas esperan en memoria a que haya sesión, permisos completados y navegación preparada. Se consumen una sola vez. Se admite también la ruta `exp://<servidor>/--/unirse/<codigo>` para comprobar la navegación con Expo Go; el QR y el enlace compartido conservan siempre el esquema del contrato.

`GameInvitation` consulta `GET /api/v1/partidas/codigo/{codigo}` mediante el cliente autenticado existente y muestra únicamente `PartidaPublica`. No presupone un `id` en esta respuesta. Incluye carga, error 404, fallo de conexión y reintento; ignora respuestas de consultas anteriores si se abre otra invitación. No crea inscripciones ni expone objetivos o misiones.

La tarea #56 incorpora `ScanInvitation`, accesible desde «Unirse» en Inicio. Usa `expo-camera` para leer solo códigos QR, solicita permiso al abrir la pantalla, explica cómo recuperarlo si está bloqueado y detiene la cámara al perder el foco. El contenido leído pasa por el mismo validador de enlaces que usa la navegación externa: solo se aceptan enlaces `ghostmark://unirse/<codigo>` o la ruta de desarrollo de Expo. Un código válido abre `GameInvitation` una sola vez; un QR ajeno muestra un error y permite seguir escaneando. El código se normaliza a mayúsculas. La inscripción, las normas y la sala de espera quedan para las siguientes tareas de la PBI 3.

Las pruebas de componentes y navegación aíslan las APIs nativas y simulan la respuesta HTTP. No sustituyen la comprobación del esquema, la cámara y el portapapeles en un dispositivo. Para verificar `ghostmark://` y el escáner hace falta instalar un binario de la app que incluya el esquema y `expo-camera`; Expo Go usa su propia URL de desarrollo.

Manual: [Compartir una partida](../manual-usuario/compartir-partida.md).
