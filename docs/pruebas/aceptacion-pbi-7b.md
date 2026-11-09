# Aceptación de la PBI 7b — Códigos rechazados y eliminaciones simultáneas

Tarea #200 (7b.3), PBI #189. Las pruebas del servidor están en
[EliminacionTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/juego/EliminacionTest.java).
Los tests de la app se añadieron en la tarea 7b.2 (#199).

| Criterio o caso | Prueba automática |
|---|---|
| 1. Se rechaza un código caducado, uno ajeno y el de una víctima ya eliminada | `unCodigoDeLosUltimosMinutosRespondeCaducado`, `unCodigoDeOtroJugadorNoElimina` y `elCodigoDeUnaVictimaYaEliminadaEsIncorrecto` |
| 1. «Eliminar» enseña el motivo del rechazo y los intentos que quedan | `life-code-screens.test.cjs`: «Eliminar muestra el rechazo y los intentos que devuelve el servidor» |
| 1. Tras 3 fallos seguidos los intentos se bloquean hasta que rota el código, también con el código correcto, y después se puede eliminar | `tresFallosBloqueanHastaQueRotaElCodigo` y `trasRotarElCodigoSeDesbloqueaYSePuedeEliminar` |
| 1. La carta lleva los intentos que quedan y el bloqueo | `laCartaLlevaLosIntentosQueQuedanYElBloqueo` |
| 1. «Eliminar» bloquea el teclado con la cuenta atrás y se abre con los intentos y el bloqueo de la carta | `life-code-screens.test.cjs`: «Eliminar bloquea el teclado hasta la fecha que devuelve el servidor», «Eliminar se abre con los intentos que quedan según la carta» y «Eliminar se abre bloqueado si la carta trae bloqueadoHasta y no deja escribir»; `mission-card.test.cjs`: «ELIMINAR abre «Eliminar» con los intentos y el bloqueo de la carta» |
| 2. Si dos eliminaciones coinciden en la cadena, solo vale la que se registra primero | `eliminacionesEnCadenaALaVezDejanElCicloCerrado` |

El reloj del servidor se fija para probar el instante anterior y el instante
exacto de la rotación sin esperar en tiempo real.

En la prueba simultánea, A intenta eliminar a B y B intenta eliminar a C. Si
A registra primero su eliminación, la petición de B devuelve
`409 JUGADOR_ELIMINADO`. Si B registra primero su jugada, ambas pueden prosperar
en ese orden. Se comprueba la secuencia de eventos y que queda un solo ciclo
cerrado entre los supervivientes.

Con Java 21 y Docker, desde `backend`:

```powershell
.\mvnw.cmd '-Dtest=EliminacionTest' test
```

Desde `mobile`, tras `npm ci`:

```powershell
node --test tests/life-code-screens.test.cjs tests/mission-card.test.cjs
```
