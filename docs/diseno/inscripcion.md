# Inscripción y lista de inscritos (3.2, #54)

Implementa los endpoints de inscripción del contrato `api/openapi.yaml` y las tablas de «Inscripción» en `modelo-datos.md`, incluidos los acuerdos de la revisión de la #84.

## API

| Operación | Acceso | Resultado |
|--|--|--|
| `POST /api/v1/inscripciones` | Cuenta autenticada | `201`: id de inscripción, partidaId, inscritaEn y horario copiado |
| `GET /api/v1/partidas/{partidaId}/inscritos` | Máster o inscrito | `200`: id de inscripción, alias y esMaster, por orden de inscripción |
| `GET /api/v1/cuentas/yo/partidas` | Cuenta autenticada | Incluye las partidas del máster y las del jugador, sin duplicados |

El alta recibe `{"codigo":"K7PX4MQ2","aceptaNormas":true}`. Sin normas aceptadas devuelve `400 NORMAS_NO_ACEPTADAS`; los datos incompletos o inválidos, `400 DATOS_NO_VALIDOS`. Un código válido inexistente da `404 NO_ENCONTRADA`. Una inscripción duplicada da `409 YA_INSCRITO`; un máster que no juega, `409 ES_MASTER`; un estado distinto de `EN_INSCRIPCION` o una hora de arranque ya alcanzada, `409 PARTIDA_NO_ABIERTA`. Las entradas tardías quedan para la PBI 16.

La lista devuelve `404` tanto si no existe la partida como si quien llama no es el máster ni está inscrito. No expone correo, cuentaId, aceptaciones ni horarios de otros. Una inscripción conservada tras borrar la cuenta aparece con `alias: null`.

## Transacciones y eventos

- La cuenta se bloquea antes de inscribirla o borrarla. El alta, la copia del horario y `JUGADOR_INSCRITO` se confirman en una transacción.
- Cada escritor de eventos bloquea la fila de partida antes de calcular `max(secuencia) + 1`. Los borrados que afectan a varias partidas adquieren esos bloqueos en orden ascendente de id.
- Si el máster juega, su inscripción se crea tras `PARTIDA_CREADA`, con las mismas fechas y su horario copiado. La creación devuelve el número real de inscritos.
- Al borrar el máster se cancelan primero sus partidas en inscripción, con el número de inscritos anterior a su retirada. Después se retiran sus inscripciones y finalmente se borra su cuenta.
- En `EN_JUEGO` se conserva el UUID de la inscripción, se anula `cuenta_id` y se borra su horario. En los demás estados se borran inscripción y horario y se añade `INSCRIPCION_RETIRADA`, con motivo `CUENTA_BORRADA`.
- Los eventos existentes nunca se reescriben. Los de inscripción solo contienen `inscripcionId` y, en una retirada, `motivo`.

## Migración

Crea `inscripcion` y `franja_horario_inscripcion`, con las claves, índices y restricciones del modelo. Da de alta al máster que juega en cada partida existente con máster y que no esté cerrada: las dos fechas son `partida.creada_en`, el horario se copia del actual de su cuenta y se añade `JUGADOR_INSCRITO` sin modificar eventos anteriores. No recrea datos individuales de partidas cerradas.

## Integración con las tareas siguientes

La 3.3 (#55) implementa consultar y modificar el horario de cada inscripción. La 2.6 (#51) crea o retira la inscripción al editar «Yo también juego». La 4.2 aplica el arranque programado y pendiente al consultar, usando el mismo bloqueo de partida; la 3.2 ya rechaza nuevas altas si se ha alcanzado `inicio_en`, pero no ejecuta el arranque ni la cancelación de la 4.2/4.4.

`InscripcionRepository` proporciona consultas y operaciones de persistencia; `ServicioInscripcion.alta` permite reutilizar el alta y su evento dentro de una transacción existente. Quien la reutilice debe validar la operación y respetar el orden cuenta → partida.

## Comprobación

`./mvnw verify` ejecuta las pruebas del backend, Checkstyle y el umbral JaCoCo del 75 %. `InscripcionTest` verifica altas, normas, copia de horario, varias partidas, autorización, errores, borrado y concurrencia. `InscripcionMigracionTest` migra un esquema aislado con partidas anteriores y comprueba el alta de másteres, fechas, horarios, eventos y restricciones. Se mantienen las pruebas previas, ajustando los recuentos al máster que ya juega desde la creación.
