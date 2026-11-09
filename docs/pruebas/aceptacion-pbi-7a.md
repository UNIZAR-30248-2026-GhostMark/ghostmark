# Aceptación de la PBI 7a — Código de vida y eliminación

Tarea #197 (7a.6), PBI #7. Pruebas de aceptación del servidor sobre las tareas
7a.2 (#193) y 7a.3 (#194). La presentación y navegación se prueban en las tareas
de app 7a.4 (#195) y 7a.5 (#196).

## Trazabilidad de los criterios

| Criterio o caso | Prueba automática |
|---|---|
| 1. Cada vivo obtiene solo su código de seis dígitos, que cambia cada 60 s, con máster que juega y máster que no juega | `EliminacionAceptacionTest.criterio1CadaVivoObtieneSoloSuCodigoYEsteRotaCada60Segundos` |
| 1. «Mi código» muestra el código y la cuenta atrás, y pide el nuevo al acabar | `life-code-screens.test.cjs`: «Mi código muestra los seis dígitos y la cuenta atrás recibidos» y «al acabar la cuenta atrás Mi código pide el código nuevo sin repetir la petición cada segundo» |
| 1. La app usa la ruta privada de su código y la cuenta atrás no baja de cero | `life-code.test.cjs` |
| 2. El código del objetivo elimina a la víctima, suma una eliminación al cazador y el feed no dice quién la hizo | `criterio2ElCodigoDelObjetivoEliminaSumaYPublicaUnFeedSinCazador` |
| 2. «Eliminar» envía los seis dígitos y el acierto dice a quién has eliminado | `life-code-screens.test.cjs`: «Eliminar envía seis dígitos y abre la pantalla de acierto» y «el acierto dice a quién has eliminado y vuelve a la misma carta, ya recargada» |
| 2. «Te han cazado» y «X fue eliminado» en el feed, sin el cazador | Pendiente de la 7a.5 (#196) |
| 3. El cazador hereda objetivo y misión, el ciclo sigue siendo único y cerrado y con dos vivos cada uno es objetivo del otro | `criterio3HeredaObjetivoYMisionYConservaUnSoloCicloHastaDosVivos` |
| 3. El acierto avisa de la herencia y vuelve a la carta | `life-code-screens.test.cjs`: «el acierto dice a quién has eliminado y vuelve a la misma carta, ya recargada» |
| 3. La carta muestra el objetivo y la misión heredados | Pendiente de la 7a.5 (#196) |

## Cómo se comprueba el servidor

Las pruebas registran cuentas, crean partidas, inscriben jugadores y arrancan
por HTTP contra la aplicación real con PostgreSQL/PostGIS de Testcontainers.
El código utilizado para eliminar se obtiene por el endpoint de la propia víctima;
las cartas, la herencia y el feed se verifican por la API pública autenticada.

Solo se controla el reloj del servidor y, en el criterio 1, se fija el secreto
de un jugador con el vector conocido `12345678901234567890`. Así se comprueban
los códigos `287082` y `359152` a ambos lados del segundo 120, sin esperas reales
ni riesgo de que dos códigos aleatorios consecutivos coincidan.

El criterio 2 verifica también que el eliminado pierde acceso al código y a
eliminar, conserva su carta como `ELIMINADO` y puede consultar el feed, igual que
el máster que no juega. El feed solo puede contener `tipo`, `alias` y `ocurridoEn`;
el texto «X fue eliminado» corresponde a la app.

El criterio 3 recorre todas las cartas de los vivos antes y después de cada
eliminación, verifica que no hay ciclos separados y reduce una partida real
de cuatro jugadores a tres y después a dos. No simula eliminaciones actualizando
la base de datos. Se detiene con dos vivos: el cierre y el ganador son de la PBI 8.
Los códigos rechazados, bloqueos y operaciones simultáneas son de la PBI 7b.

## Ejecución

Con Java 21 y Docker disponibles, desde `backend`:

```powershell
.\mvnw.cmd clean verify
```

Para ejecutar únicamente esta clase durante el desarrollo:

```powershell
.\mvnw.cmd -Dtest=EliminacionAceptacionTest test
```

El flujo `ci-backend` ya ejecuta `verify`; no necesita cambios. Las pruebas de la
app de la #195 se ejecutan desde `mobile` tras `npm ci`:

```powershell
node --test tests/life-code.test.cjs tests/life-code-screens.test.cjs
```
