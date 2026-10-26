# Aceptación de la PBI 5 — Objetivo y misión

Tarea #70 (5.4), PBI #31. Pruebas en `CartaAceptacionTest`, sobre la implementación
del servidor de la 5.2. No requieren la pantalla de la 5.3.

| Criterio o caso | Prueba automática |
|---|---|
| 1. Cada jugador consulta exclusivamente su objetivo y su misión, incluido el máster que juega | `criterio1CadaJugadorVeSoloSuObjetivoYSuMisionIncluidoElMaster` |
| 2. Todos reciben al arrancar una misión de la biblioteca curada, sin contacto físico ni objetos que simulen armas | `criterio2AlArrancarTodosRecibenUnaMisionDeLaBibliotecaCuradaSegura` |
| Privacidad por rol: jugador, máster que juega, máster que no juega y no inscrito | `ningunRolObtieneElObjetivoNiLaMisionDeOtro` (cuatro casos) |

Las pruebas crean cuentas, partidas e inscripciones mediante HTTP y arrancan la partida
por su API. La primera contrasta la carta con el ciclo y el reparto registrados.
La prueba por rol comprueba la suplantación mediante parámetros, el acceso por una ruta
de carta ajena y que la lista de inscritos no expone objetivos ni misiones.

La seguridad del contenido se comprueba contra los textos revisados de la biblioteca
inicial de la 5.1. No se pretende deducir el significado de una misión mediante palabras
prohibidas. Una misión nueva o modificada requiere revisar que cumple ambos límites
antes de incorporarla a `MISIONES_REVISADAS`.

Con Java 21 y Docker disponibles, ejecutar desde `backend`:

```powershell
.\mvnw.cmd clean verify
```

Surefire descubre la clase por su nombre terminado en `Test`. El flujo existente
`ci-backend` ejecuta `verify`, por lo que no necesita cambios.
