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

Las interacciones visuales del mapa (`e4-zonas`) y la pestaña Partida
(`f-partida-zonas`) pertenecen respectivamente a las tareas de app #282 y #283.
No se dan por verificadas aquí: sus tests de interfaz deberán comprobar el
dibujo y la lectura sin mostrar posiciones personales. Esta tarea verifica el
contrato del servidor sobre el que se apoyan dichas pantallas.

## Ejecución

Con Java 21 y Docker disponibles, desde `backend`:

```powershell
.\mvnw.cmd '-Dtest=AceptacionZonasTest,ZonasTest' test
.\mvnw.cmd clean verify
```

El flujo `ci-backend` ejecuta `verify`; no necesita cambios para estas pruebas.
