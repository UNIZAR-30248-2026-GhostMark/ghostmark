# Aceptación de la PBI 7a — Código de vida y eliminación

Tarea #197 (7a.6), PBI #7. Pruebas de aceptación del servidor sobre las tareas
7a.2 (#193) y 7a.3 (#194). La presentación y navegación se prueban en las tareas
de app 7a.4 (#195) y 7a.5 (#196).

## Trazabilidad de los criterios

| Criterio de satisfacción | Tests del servidor | Tests de la app y cobertura pendiente |
|---|---|---|
| 1. Cada vivo ve su código de seis dígitos, cambia cada 60 s y solo él puede obtenerlo | [`EliminacionAceptacionTest`](../../backend/src/test/java/es/unizar/ghostmark/backend/juego/EliminacionAceptacionTest.java), `criterio1CadaVivoObtieneSoloSuCodigoYEsteRotaCada60Segundos` (máster que juega y máster que no juega) | #195: [`life-code-screens.test.cjs`](../../mobile/tests/life-code-screens.test.cjs), «Mi código muestra los seis dígitos y la cuenta atrás recibidos» y «al acabar la cuenta atrás Mi código pide el código nuevo sin repetir la petición cada segundo». [`life-code.test.cjs`](../../mobile/tests/life-code.test.cjs) comprueba las rutas privadas y la cuenta atrás |
| 2. El código del objetivo elimina a la víctima, suma una eliminación al cazador y el feed no revela quién la hizo | [`EliminacionAceptacionTest`](../../backend/src/test/java/es/unizar/ghostmark/backend/juego/EliminacionAceptacionTest.java), `criterio2ElCodigoDelObjetivoEliminaSumaYPublicaUnFeedSinCazador` | #195: [`life-code-screens.test.cjs`](../../mobile/tests/life-code-screens.test.cjs), «Eliminar envía seis dígitos y abre la pantalla de acierto» y «el acierto dice a quién has eliminado y vuelve a la misma carta, ya recargada». **Pendiente #196:** enlazar los tests de «Te han cazado» y del texto «X fue eliminado» sin cazador en el feed |
| 3. El cazador hereda objetivo y misión, el ciclo sigue siendo único y cerrado y con dos vivos cada uno es objetivo del otro | [`EliminacionAceptacionTest`](../../backend/src/test/java/es/unizar/ghostmark/backend/juego/EliminacionAceptacionTest.java), `criterio3HeredaObjetivoYMisionYConservaUnSoloCicloHastaDosVivos` | #195: [`life-code-screens.test.cjs`](../../mobile/tests/life-code-screens.test.cjs), «el acierto dice a quién has eliminado y vuelve a la misma carta, ya recargada», comprueba el aviso de herencia y la navegación de vuelta. **Pendiente #196:** enlazar la prueba que compruebe el objetivo y la misión heredados en la carta |

Los enlaces de la #195 apuntan a los archivos de su tarea y estarán disponibles
en la rama común cuando se integre. La #196 aún no está implementada: no se
consideran verificadas sus pantallas ni se da por cerrada la aceptación completa
de la PBI. Cuando se entregue, deben sustituirse los pendientes por enlaces a
sus tests reales y comprobar su ejecución.

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

El flujo `ci-backend` ya ejecuta `verify`; no necesita cambios. En la rama que
contenga la #195, sus pruebas de app pueden ejecutarse desde `mobile` tras `npm ci`:

```powershell
node --test tests/life-code.test.cjs tests/life-code-screens.test.cjs
```

Esta tabla describe la cobertura y sus pendientes; no certifica una ejecución
ni sustituye la revisión ni la prueba de las pantallas en Android.
