# Aceptación de la PBI 1 — Cuenta del jugador

PBI #2. Los tests son los de las tareas del sprint 1; esta tabla los enlaza con cada criterio (#188).

| Criterio o caso | Test |
|---|---|
| 1. Crear la cuenta e iniciar sesión; con datos incorrectos no entra | `CuentaIntegracionTest.criterio1CrearCuentaEIniciarSesionYRechazarDatosIncorrectos` |
| 1. Alias o email repetidos y datos mal formados dan error | `SesionTest.duplicadosYDatosMalDanError` y `loginConDatosMalFormadosDa400` |
| 1. La contraseña no se guarda legible | `CuentaIntegracionTest.contrasenaNoSeGuardaLegible` |
| 1. El refresco cambia el token y cerrar sesión lo revoca | `SesionTest.refrescoRotaYCierreRevoca` y `cierreConTokenAjenoNoLoRevoca` |
| 1. Una ruta protegida sin token da 401 | `SesionTest.rutaProtegidaSinTokenDa401NoAutenticado` |
| 1. La app valida el alta y el inicio antes de enviarlos | `account-validation.test.cjs`: «el alta valida alias, email y contraseña sin pedir nada al servidor» y «el inicio valida email y contraseña vacía antes de enviarlos» |
| 1. La app renueva la sesión una sola vez con varias peticiones a la vez y guarda el token nuevo | `session.test.cjs` |
| 1. Sin sesión la app abre en «Iniciar sesión» | `invitation.test.cjs`: «sin sesión la app abre en «Iniciar sesión»» |
| 2. Horario y aceptaciones se piden una sola vez | `CuentaIntegracionTest.criterio2HorarioYAceptacionesQueSePidenUnaSolaVez`; `AceptacionesTest.registraAceptacionesSoloUnaVez` |
| 2. La cuenta solo guarda alias, datos de acceso, horario y aceptaciones | `CuentaIntegracionTest.criterio2LaCuentaSoloGuardaAliasDatosDeAccesoYAceptaciones` (los amigos llegan con la PBI 15) |
| 2. Guardar, sustituir y vaciar el horario; un horario no válido no cambia el anterior | `HorarioTest.guardarYConsultarHorario`, `sustituirYVaciarHorario` y `rechazarHorarioInvalidoSinModificarAnterior` |
| 2. La app pasa al aviso o al permiso que falte, y a Inicio con los dos aceptados | `session.test.cjs` (`nextAccountScreen`) |
| 2. La app explica el permiso de ubicación y ofrece los ajustes si Android ya no deja preguntar | `location.test.cjs` |
| 2. La app avisa al añadir, unir y quitar franjas del horario | `day-schedule-modal.test.cjs` |
| 3. Borrar la cuenta impide iniciar sesión y libera el alias | `CuentaIntegracionTest.criterio3BorrarLaCuentaImpideIniciarSesionYLiberaElAlias`; `BorradoCuentaTest.borrarCuentaEliminaDatosEImpideIniciarSesion` |
| 3. Tras borrar la cuenta, la app vuelve a «Iniciar sesión» | `invitation.test.cjs`: «cerrar sesión y borrar la cuenta llevan a «Iniciar sesión»» |

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

`ci-backend` descubre los tests Java y `ci-mobile` ejecuta los de la app con `npm test`. Los tests del servidor usan HTTP real y PostgreSQL de Testcontainers; los de la app montan las pantallas y el cliente HTTP reales con la red simulada.
