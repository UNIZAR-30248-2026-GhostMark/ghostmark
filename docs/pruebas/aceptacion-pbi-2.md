# Aceptación de la PBI 2 — Crear y configurar una partida

PBI #3. Los tests son los de las tareas del sprint 1; esta tabla los enlaza con cada criterio (#188).

| Criterio o caso | Test |
|---|---|
| 1. La partida se guarda con sus valores y se rechaza un fin anterior al inicio | `PartidaIntegracionTest.criterio1CrearPartidaGuardaSusValoresYRechazaFinAnteriorAlInicio` |
| 1. Crear devuelve el máster y el código, y guarda el evento `PARTIDA_CREADA` | `PartidaTest.crearPartidaDevuelve201ConMasterYCodigo` y `crearPartidaGuardaEventoPartidaCreada` |
| 1. Fin antes del inicio, inicio a menos de 15 minutos, reglas no válidas y cuerpo vacío dan 400 | `PartidaTest.finAntesDelInicioDa400`, `inicioDemasiadoProntoDa400`, `reglasNoValidasDan400` y `cuerpoVacioDa400ConTodosLosCampos` |
| 1. Solo el máster ve y cambia la configuración, y cada cambio guarda su evento | `PartidaIntegracionTest.soloElMasterVeYCambiaLaConfiguracion` y `elMasterCambiaLaConfiguracionYSeGuardaElEvento` |
| 1. Cambios no válidos o con la partida ya arrancada no se guardan | `PartidaIntegracionTest.cambiosNoValidosDan400YNoSeGuardan` y `partidaQueYaNoEstaEnInscripcionDa409` |
| 1. «Yo también juego» crea y borra la inscripción del máster | `PartidaIntegracionTest.yoTambienJuegoCreaYBorraLaInscripcionDelMaster` |
| 1. Quien no es máster ni inscrito no ve la partida | `PartidaIntegracionTest.quienNoEsMasterNiInscritoNoVeLaPartida` |
| 1. Si el máster borra su cuenta, sus partidas en inscripción se cancelan | `PartidaCanceladaTest.borrarCuentaDelMasterCancelaPartidasEnInscripcion` |
| 1. El asistente de la app muestra sus tres pasos y el nombre empieza vacío | `wizard.test.cjs`: «el asistente muestra los tres pasos que existen sin saltar del 2 al 5»; `invitation.test.cjs`: «el nombre de la partida empieza vacío, con «Killer Bienvenida EINA» como ejemplo» |
| 1. Crear la partida en la app abre «Compartir» | `invitation.test.cjs`: «crear una partida abre compartir y retira los formularios del historial» |
| 2. El enlace del QR abre esa partida | `PartidaIntegracionTest.criterio2ElEnlaceDelQrAbreEsaPartida`; `PartidaCodigoTest.jugadorAutenticadoConsultaPartidaPorCodigo` y `codigoDesconocidoDa404` |
| 2. El QR y «Copiar» usan el enlace del servidor | `invitation.test.cjs`: «el QR y copiar usan el enlace del servidor; ir a la partida abre la sala» |
| 2. La app solo acepta enlaces `ghostmark://unirse/<código>` válidos | `invitation.test.cjs`: «normaliza minúsculas en enlaces válidos» y «rechaza esquemas, rutas, longitudes y caracteres ajenos al contrato» |
| 2. El enlace abre la partida con la app cerrada o abierta | `invitation.test.cjs`: «un enlace con la app cerrada abre la partida tras restaurar sesión y preparar el navegador» y «con la app abierta se pueden abrir dos invitaciones consecutivas» |

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
