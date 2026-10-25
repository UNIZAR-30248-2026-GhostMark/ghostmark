# Aceptación de la PBI 4 — Arranque y ciclo

Tarea 4.6 (#65), PBI #5. Requiere la 4.3, la 4.4 y la 4.5 (#62, #63 y #64).

| Criterio o caso | Test |
|---|---|
| 1. Arranque automático en la fecha de inicio | `ArranqueAceptacionTest.criterio1ArrancaSolaEnLaFechaDeInicio` |
| 1. Arranque adelantado por el máster | `ArranqueAceptacionTest.criterio1ElMasterPuedeAdelantarElArranque` |
| 1. El inscrito ve la partida iniciada en la sala | `start-acceptance.test.cjs`: criterio 1 |
| 2. Un único ciclo cerrado, objetivo y perseguidor únicos, sin perseguirse a sí mismo | `criterio2CadaJugadorTieneUnObjetivoYUnPerseguidorEnUnSoloCiclo`: 3, 4 y 10 jugadores |
| 3. Cancelación sin arrancar, visible para los inscritos | `criterio3ConMenosDeTresSeCancelaYLosInscritosLaVenAlConsultar`: 0, 1 y 2 jugadores |
| 3. Cancelación visible al abrir la app, también fuera de la tarjeta destacada | `start-acceptance.test.cjs`: criterio 3, dos casos |
| Estado e inscripciones reconstruidos aplicando los eventos por secuencia | `elEstadoSeReconstruyeDesdeLosEventosEnOrden`: inscripción con retirada, en juego y cancelada |
| Borrado en juego sin perder la inscripción ni alterar el historial | `BorradoCuentaTest.borrarCuentaEnJuegoConservaLosEventosYDejaLaInscripcionSinAlias` |
| Representación de alias nulo como «Cuenta eliminada» | Test existente de `inscription.test.cjs`: «la sala muestra los inscritos reales y “Cuenta eliminada” si borró la cuenta» |

## Eventos y borrado de cuenta

La frase antigua de #65 sobre cambiar el alias en los eventos se aplica según el contrato
vigente de `modelo-datos.md`: los eventos son inmutables e identifican por `inscripcionId`,
sin alias ni identificadores de cuenta. El test compara todas sus columnas antes y después
del borrado, comprueba que la inscripción permanece con `cuenta_id = null` y que la API
devuelve su alias nulo. La app representa ese valor como «Cuenta eliminada».
No se reescribe el JSON histórico ni se crea una API de eventos.

El intérprete de prueba reconstruye estado, inscripciones y objetivos desde cero, sin leer
`partida.estado` para calcularlos; después contrasta el resultado con los datos persistidos
y el resumen de la API. Las misiones pertenecen a la PBI 5 y no son una dependencia de esta tarea.

## Ejecución

Con Java 21 y Docker disponibles, desde la raíz:

```powershell
cd backend
.\mvnw.cmd clean verify
cd ../mobile
npm ci
npm run typecheck
npm run lint
npm test
cd ..
```

El flujo existente `ci-backend` descubre los tests Java y `ci-mobile` ejecuta el nuevo
fichero a través de `npm test`. Los tests móviles montan las pantallas y el cliente HTTP
reales con el transporte de red simulado; no son una prueba contra un servidor o dispositivo.
