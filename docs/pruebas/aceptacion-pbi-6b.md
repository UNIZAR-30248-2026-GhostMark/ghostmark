# Aceptación de la PBI 6b — Biblioteca si la IA falla

Tarea #211 (6b.3), PBI #190. Las pruebas del servidor están en
[MisionesIaTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/mision/MisionesIaTest.java),
[RepartoMisionesIaTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/juego/RepartoMisionesIaTest.java)
y [GeneradorGeminiTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/mision/GeneradorGeminiTest.java),
y las de la app en [missions-ai.test.cjs](../../mobile/tests/missions-ai.test.cjs).

| Criterio o caso | Prueba automática |
|---|---|
| 1. Con la IA caída o lenta, la partida creada con IA arranca con misiones de la biblioteca y no se bloquea | `RepartoMisionesIaTest.siFallaLaIaLaMismaPartidaArrancaConLaBiblioteca`, en los dos modos |
| 1. Si el máster pasa la misma partida a Biblioteca después del fallo, arranca y cada carta lleva una misión de la biblioteca | `MisionesIaTest.elMasterRecibeElAvisoYCambiaLaMismaPartidaABiblioteca`, en los dos modos |
| 1. Si falla otra tanda, las aprobadas de antes se siguen repartiendo y el resto sale de la biblioteca | `RepartoMisionesIaTest.conIaSeRepartenPrimeroLasAprobadasYElRestoDeLaBiblioteca` |
| 2. Con la IA caída o lenta, pedir una tanda responde 503 con el aviso y la tanda no cuenta | `MisionesIaTest.elMasterRecibeElAvisoYCambiaLaMismaPartidaABiblioteca` y `MisionesIaTest.siLaIaNoRespondeLaTandaNoCuenta` |
| 2. Sin cuota, Gemini cuenta como no disponible | `GeneradorGeminiTest.sinCuotaNoEstaDisponible` |
| 2. El máster sigue con la misma partida: al pasarla a Biblioteca conserva su identificador y su código de invitación | `MisionesIaTest.elMasterRecibeElAvisoYCambiaLaMismaPartidaABiblioteca` |
| 2. «Misiones» avisa y deja continuar con la biblioteca; reintentar no crea otra partida (R11-19) | `missions-ai.test.cjs`: «si la IA no está disponible, Misiones avisa y permite continuar con la biblioteca» y «si falla la tanda, reintentar no crea otra partida y pasar a Biblioteca cambia la misma (R11-19)» |
| 2. Si falla otra tanda, avisa y deja continuar con las aprobadas o, sin ninguna, con el texto del criterio 2 (R11-30) | `missions-ai.test.cjs`: «si falla otra tanda, avisa y deja continuar con las misiones ya aprobadas» y «si falla otra tanda sin ninguna aprobada, avisa con el texto del criterio 2 de la 6b (R11-30)» |

La prueba repite el flujo por HTTP con el simulado en `CAIDO` y en `LENTO`.
`LENTO` lanza el fallo al momento, sin esperar el límite. El servidor sigue en
ese modo al pasar la partida a Biblioteca, inscribir a los jugadores y arrancar.

Con Java 21 y Docker, desde `backend`:

```powershell
.\mvnw.cmd '-Dtest=MisionesIaTest,RepartoMisionesIaTest,GeneradorGeminiTest' test
```

Desde `mobile`, tras `npm ci`:

```powershell
node --test tests/missions-ai.test.cjs
```
