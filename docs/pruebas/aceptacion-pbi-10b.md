# Aceptación de la PBI 10b — La zona se comprueba al eliminar

Tarea #288 (10b.3), PBI #272. La aceptación comprueba la implementación
del servidor #286 (10b.1) y la app #287 (10b.2), ambas integradas en `main`.

## Trazabilidad de los criterios

| Criterio o caso | Prueba | Resultado esperado |
|---|---|---|
| 1. Resultado `DENTRO` | `AceptacionResultadoZonaTest.criterio1CadaResultadoSeGuardaSinCoordenadasYSeAcepta` (`DENTRO`) | HTTP 200, víctima eliminada y evento con `resultadoZona=DENTRO` |
| 1. Resultado `FUERA` | El mismo test parametrizado (`FUERA`) | HTTP 200, víctima eliminada y evento con `resultadoZona=FUERA` |
| 1. Resultado `NO_CONCLUYENTE` | El mismo test parametrizado (`NO_CONCLUYENTE`) | HTTP 200, víctima eliminada y evento con `resultadoZona=NO_CONCLUYENTE` |
| 1. Nunca se almacena la posición | `criterio1CadaResultadoSeGuardaSinCoordenadasYSeAcepta` y `criterio1LaApiRechazaCoordenadasSinGuardarUnaEliminacion` | El evento conserva únicamente el resultado y la API rechaza latitud, longitud y precisión |
| 1. Resultado obligatorio y válido | `criterio1UnResultadoAusenteONoValidoSeRechazaSinGastarIntentos` | HTTP 400, sin gastar intentos ni guardar una eliminación |
| 1. El móvil calcula la zona y manda solo el resultado | `mobile/tests/zone-check.test.cjs`: «dentro, fuera y no concluyente según el círculo de la precisión» y «al meter el código se mide con las zonas de la partida y se manda solo el resultado» | Se calcula `DENTRO`, `FUERA` o `NO_CONCLUYENTE` en la app y la petición de eliminación envía únicamente el resultado de zona, sin coordenadas |
| 2. Sin permiso no se permite introducir un código | `mobile/tests/zone-check.test.cjs`: «sin permiso de ubicación no se puede meter el código hasta darlo (criterio 2)» | No aparece el teclado sin permiso; se habilita al concederlo |
| 2. Permiso denegado permanentemente | `mobile/tests/zone-check.test.cjs`: «si se negó para siempre, ofrece abrir los ajustes y sigue sin teclado» | Se ofrece abrir ajustes sin permitir introducir el código |

## Cómo se comprueba el servidor

Los tests registran cuentas, crean una partida, inscriben tres jugadores y
arrancan la partida a través de HTTP contra Spring Boot y PostgreSQL/PostGIS
de Testcontainers. Con el código válido de la víctima se comprueba que todos
los resultados de zona permiten eliminar. La aceptación se verifica mediante
la respuesta, el estado de la víctima y el evento `JUGADOR_ELIMINADO`, que
solo incluye `resultadoZona`, `origen` y los identificadores propios de la eliminación.
No se envían ni guardan coordenadas.

También se comprueba que mandar coordenadas o un resultado ausente/inválido
produce `400 DATOS_NO_VALIDOS` sin gastar intentos ni crear un evento.
El reloj del servidor está controlado para que las pruebas sean repetibles.

## Cómo se comprueba la app

La implementación de la #287 está integrada en `main`. Los tests de
`mobile/tests/zone-check.test.cjs` verifican el cálculo de `DENTRO`, `FUERA` y
`NO_CONCLUYENTE` a partir de las zonas y la precisión de la ubicación, y que
al introducir el código se envía solo el resultado de zona al servidor, sin
coordenadas. También comprueban que, sin permiso de ubicación, no aparece el
teclado hasta concederlo; si el permiso está denegado permanentemente, la app
ofrece abrir los ajustes y mantiene bloqueada la introducción del código.

Desde `mobile`, con las dependencias instaladas:

```powershell
node --test tests/zone-check.test.cjs
```

Para instalar las dependencias antes de la prueba, ejecutar `npm.cmd ci`.

La PBI 11a trata las consecuencias del resultado de zona en una disputa.

## Ejecución

Con Java 21 y Docker, desde `backend`:

```powershell
.\mvnw.cmd '-Dtest=AceptacionResultadoZonaTest,EliminacionTest' test
.\mvnw.cmd -B clean verify
```
