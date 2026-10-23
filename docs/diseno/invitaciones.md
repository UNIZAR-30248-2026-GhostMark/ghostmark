# Invitaciones: compartir y abrir una partida (#50)

El servidor devuelve `codigoInvitacion` y `enlace` al crear la partida. El móvil construye el enlace canónico `ghostmark://unirse/<codigoInvitacion>` del contrato, con ocho caracteres del alfabeto `23456789ABCDEFGHJKMNPQRSTUVWXYZ`.

`CreateGameRules` crea la partida una sola vez y sustituye el historial del asistente por `Home → ShareGame`. Compartir no vuelve a realizar el POST y volver atrás no permite reenviar el formulario ya completado. `ShareGame` presenta el paso 5 del prototipo `f2-e5`, un QR generado localmente, el enlace seleccionable, copia al portapapeles y el menú nativo para compartir. El QR tiene margen blanco y la ilustración queda fuera de sus módulos.

`app.json` registra el esquema `ghostmark`. El navegador recibe tanto la URL inicial como los eventos de URL con la aplicación abierta. Las invitaciones válidas esperan en memoria a que haya sesión, permisos completados y navegación preparada. Se consumen una sola vez. Se admite también la ruta `exp://<servidor>/--/unirse/<codigo>` para comprobar la navegación con Expo Go; el QR y el enlace compartido conservan siempre el esquema del contrato.

`GameInvitation` consulta `GET /api/v1/partidas/codigo/{codigo}` mediante el cliente autenticado existente y muestra únicamente `PartidaPublica`. No presupone un `id` en esta respuesta. Incluye carga, error 404, fallo de conexión y reintento; ignora respuestas de consultas anteriores si se abre otra invitación. No crea inscripciones ni expone objetivos o misiones. El escáner y la inscripción se desarrollan en la PBI 3.

Las pruebas de componentes y navegación aíslan las APIs nativas y simulan la respuesta HTTP. No sustituyen la comprobación del esquema registrado, el portapapeles y el menú de compartir en un dispositivo. Para verificar `ghostmark://` hace falta instalar un binario de la app que incluya el esquema; Expo Go usa su propia URL de desarrollo.

Manual: [Compartir una partida](../manual-usuario/compartir-partida.md).
