# Aceptación de la PBI 6a — Misiones generadas con IA

Tarea #209 (6a.4), PBI #6. Las pruebas del servidor están en
[MisionesIaTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/mision/MisionesIaTest.java),
[FiltroMisionesTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/mision/FiltroMisionesTest.java),
[GeneradorGeminiTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/mision/GeneradorGeminiTest.java)
y [RepartoMisionesIaTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/juego/RepartoMisionesIaTest.java),
y las de la app en [missions-ai.test.cjs](../../mobile/tests/missions-ai.test.cjs).

| Criterio o caso | Prueba automática |
|---|---|
| 1. El máster pide una tanda con un contexto y aprueba o rechaza cada misión; solo él puede | `elMasterPideUnaTandaYRevisaSusMisiones` y `soloElMasterGestionaLasMisionesDeLaIa` |
| 1. «Misiones» pide la tanda con el contexto elegido o escrito y la pila aprueba o rechaza una a una | `missions-ai.test.cjs`: «Misiones con IA crea la partida una vez y pide la tanda con el chip elegido», ««Otro…» manda el contexto escrito sin los espacios de los lados» y «la pila aprueba con ✓ o deslizando a la derecha y rechaza con ✗ o deslizando a la izquierda» |
| 1. La decisión del máster es definitiva: si el servidor la rechaza, la carta se queda (R11-18) | `missions-ai.test.cjs`: «si el servidor rechaza la decisión, la carta se queda y sale el error (R11-18)» |
| 1. Ninguna misión entra en juego sin aprobar: al arrancar se reparten las aprobadas y el resto sale de la biblioteca | `lasAprobadasSeRepartenAlArrancar` y `RepartoMisionesIaTest.conIaSeRepartenPrimeroLasAprobadasYElRestoDeLaBiblioteca` |
| 2. Una propuesta inválida o prohibida no llega al máster, ni en la tanda ni al volver a listar | `lasDescartadasNoLleganAlMasterPeroLaTandaCuenta` |
| 2. El filtro descarta las categorías no permitidas, las palabras de la lista y el formato inválido | `FiltroMisionesTest`: `descartaLosSaltosDeLineaYLasCategoriasQueNoEstanPermitidas`, `ningunaPalabraDeLaListaPasaElFiltro` y `quitaLoQueDevuelveElSimuladoConFormatoInvalidoOProhibidas` |
| 2. Una respuesta de Gemini que no se entiende no da misiones | `GeneradorGeminiTest.unJsonQueNoSeEntiendeNoDaMisiones` |
| 2. Una tanda sin ninguna válida avisa y deja pedir otra o continuar (R11-22) | `missions-ai.test.cjs`: «una tanda vacía avisa de que no hay misiones válidas y deja pedir otra o continuar (R11-22)» |

El servidor usa el generador simulado. Las tandas se piden por HTTP y se
comprueba qué textos llegan en la respuesta y cuáles aparecen al volver a
listar. Para el reparto se aprueban dos misiones, se rechaza otra y se dejan las
demás sin revisar. Se arranca la partida y se comprueba qué se repartió.

Con Java 21 y Docker, desde `backend`:

```powershell
.\mvnw.cmd '-Dtest=MisionesIaTest,RepartoMisionesIaTest,FiltroMisionesTest,GeneradorGeminiTest' test
```

Desde `mobile`:

```powershell
node --test tests/missions-ai.test.cjs
```
