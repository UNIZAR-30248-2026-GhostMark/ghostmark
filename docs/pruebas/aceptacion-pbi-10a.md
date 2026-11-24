# Aceptación de la PBI 10a — Zonas seguras en el mapa

Tarea #284 (10a.5), PBI #10. Pruebas de aceptación del contrato de servidor
implementado en la 10a.2 (#281):
[AceptacionZonasTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/zonas/AceptacionZonasTest.java).
Las validaciones de geometrías, permisos e identificadores también se prueban en
[ZonasTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/zonas/ZonasTest.java).

## Trazabilidad de los criterios

| Criterio o caso | Prueba automática |
|---|---|
| 1. El máster crea una zona en inscripción y queda guardada | `criterio1ElMasterCreaUnaZonaYQuedaGuardadaEnLaPartida` |
| 1. El máster modifica nombre y polígono, conservando la zona | `criterio1ElMasterModificaNombreYPoligonoConservandoLaZona` |
| 1. El máster borra una zona y puede vaciar la lista | `criterio1ElMasterBorraUnaZonaYPuedeBorrarlasTodas` |
| 1. Las zonas valen para todos los inscritos, incluido el máster que no juega, sin filtrarse por jugador ni confundirse entre partidas | `criterio1LasZonasSonLasMismasParaTodosLosInscritos` |
| 1. Tras el arranque no se pueden añadir, modificar ni borrar zonas; los jugadores las siguen viendo | `criterio1ElArranqueRealCongelaLasZonasPeroPermiteConsultarlas` |
| 2. La lista contiene solo el dibujo (id, nombre y vértices) y no posiciones de jugadores | `criterio2ElListadoSoloContieneElDibujoSinPosicionesDeJugadores` |
| 1. Geometrías válidas, límites y permisos de edición | `ZonasTest.geometriasInvalidasDan400YSinEfectosParciales`, `ZonasTest.nombresRepetidosYLimiteDeZonasDan400SinGuardarNada`, `ZonasTest.listadoVisibleParaMasterYJugadorPeroNoParaOtraCuenta` |
| 1. En la app, el máster dibuja, deshace, termina, modifica y borra zonas en el mapa (`e4-zonas`) | `mobile/tests/zones.test.cjs`: «el paso de zonas permite deshacer, terminar, editar y borrar antes de guardar» y «el mapa muestra los vértices, anticipa el cierre de la zona y no reajusta el zoom al editar» |
| 1. En la app, la sala de espera carga las zonas, guarda los cambios y vuelve | `mobile/tests/zones-editor.test.cjs`: «desde la sala de espera carga las zonas de la partida, «GUARDAR» las guarda y vuelve (R12-32)» |
| 1. En la app, se respetan el máximo de zonas y los puntos del polígono al editar | `mobile/tests/zones-editor.test.cjs`: «con 10 zonas no deja empezar otra, pero sí editar una de ellas» y «si se deshacen todos los puntos de la zona que se editaba, vuelve a la lista tal como estaba» |
| 1. En la app, no se pueden guardar modificaciones tras el arranque | `mobile/tests/zones-editor.test.cjs`: «si la partida ya arrancó, «GUARDAR» lo explica y no vuelve» |
| 1. El cliente consulta y sustituye las zonas de la partida mediante la API | `mobile/tests/zones.test.cjs`: «el cliente lee y sustituye las zonas completas por partida» |
| 2. El mapa de edición comunica solo puntos dibujados y no consulta la ubicación del jugador | `mobile/tests/zones.test.cjs`: «el mapa no consulta la API ni la ubicación y comunica al móvil solo puntos del dibujo» |
| 2. En la pestaña Partida (`f-partida-zonas`), las zonas se ven en un mapa de solo lectura con cuántas hay, y sin zonas no sale el bloque | `mobile/tests/game-tab-zones.test.cjs`: «sin zonas en la partida la pestaña Partida no enseña el bloque de zonas», «con una zona enseña el mapa de solo lectura, «1 zona» y la nota de que nadie ve dónde estás» y «con varias zonas dice cuántas hay y «VER MAPA» abre el mapa entero con las mismas zonas» |
| 2. El mapa entero de zonas es de solo lectura y vuelve a Partida | `mobile/tests/game-tab-zones.test.cjs`: «el mapa entero de zonas es de solo lectura, sin dibujo, y vuelve a Partida» |
| 2. Si no se pueden leer las zonas, la pestaña Partida sigue con los jugadores | `mobile/tests/game-tab-zones.test.cjs`: «si no se pueden leer las zonas, la pestaña Partida sigue con los jugadores y sin el bloque» |
| 2. Ni la pestaña Partida ni el mapa entero piden la ubicación de nadie | `mobile/tests/game-tab-zones.test.cjs`: «ni la pestaña Partida ni el mapa entero piden la ubicación de nadie» |

## Alcance

Las pruebas recorren la API autenticada mediante HTTP contra Spring Boot y
PostgreSQL/PostGIS de Testcontainers. Registran cuentas, crean partidas e
inscriben jugadores de verdad. Para comprobar el bloqueo se utiliza el endpoint
real de arranque con tres inscritos, no una actualización artificial del estado.
Se verifica la persistencia y el evento `ZONAS_CAMBIADAS`. Se comparan las
respuestas de varios jugadores para comprobar que todos reciben las mismas zonas.

Los vértices (`lat`, `lon`) representan **exclusivamente el polígono dibujado
por el máster**, no una ubicación medida de ningún jugador. El test del criterio
2 verifica que la respuesta tiene únicamente `zonas` y que cada elemento solo
contiene `id`, `nombre` y `puntos`, con `lat` y `lon` en cada vértice.

La edición visual del mapa (`e4-zonas`), implementada en la tarea de app #282,
se comprueba con `mobile/tests/zones.test.cjs` y
`mobile/tests/zones-editor.test.cjs`. Estos tests cubren el dibujo y sus
controles, la edición desde la sala de espera, el guardado y el bloqueo al
arrancar. La vista de solo lectura en la pestaña Partida (`f-partida-zonas`)
y el mapa entero, de la tarea de app #283, se comprueban con
`mobile/tests/game-tab-zones.test.cjs`: enseñan las zonas sin dibujo ni
posiciones de jugadores y no piden la ubicación.

Las comprobaciones del servidor y de la app son complementarias: los tests
móviles no sustituyen la validación de permisos y persistencia de la API.

## Ejecución

Con Java 21 y Docker disponibles, desde `backend`:

```powershell
.\mvnw.cmd '-Dtest=AceptacionZonasTest,ZonasTest' test
.\mvnw.cmd clean verify
```

El flujo `ci-backend` ejecuta `verify`; no necesita cambios para estas pruebas.

Las pruebas de la app de la #282 y la #283, desde `mobile` y con las
dependencias instaladas (`npm.cmd ci`):

```powershell
node --test tests/zones.test.cjs tests/zones-editor.test.cjs tests/game-tab-zones.test.cjs
```
