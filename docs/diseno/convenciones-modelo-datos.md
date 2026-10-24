# Convenciones del modelo de datos y de la API

Reglas comunes para que las tablas, migraciones y endpoints que crea cada persona sigan el mismo estilo. Se aplican desde el sprint 1; un cambio en ellas se discute en la retrospectiva y se hace en una *pull request* propia.

## Idioma

- Tablas, columnas, campos JSON, rutas de la API y clases de dominio, **en español sin tildes ni ñ**: `cuenta`, `partida`, `inscripcion`, `franja_horario`, `anio`. Coinciden con el glosario del producto y con los issues.
- Lo propio del framework sigue en inglés: `config`, `security`, `health`, `Controller`, `Repository`.

## Base de datos (PostgreSQL con PostGIS)

| Elemento | Regla | Ejemplo |
|--|--|--|
| Tablas | `snake_case`, en singular | `cuenta`, `franja_horario` |
| Columnas | `snake_case` | `hora_inicio`, `hash_contrasena` |
| Clave primaria | `id uuid`, generada por el servidor | `id uuid primary key` |
| Clave ajena | `<tabla>_id`, con `foreign key` e índice | `cuenta_id uuid references cuenta(id)` |
| Tablas que solo son un conjunto de valores de otra | Clave primaria compuesta, sin `id` | `franja_horario (cuenta_id, dia, hora_inicio)` |
| Instantes | `timestamptz`, siempre en UTC; nombre en participio con `_en` | `creada_en`, `aceptado_en`, `caduca_en` |
| Hora de un horario semanal | `time` sin zona; se interpreta en `Europe/Madrid` | `hora_inicio time` |
| Día de la semana | `smallint` de 1 (lunes) a 7 (domingo) | `dia smallint` |
| Hechos que ocurren una vez | Instante en que ocurrió, nulo si no ha ocurrido, en vez de un booleano | `ubicacion_aceptada_en` |
| Estados y tipos | `varchar` con `check`, valores en MAYÚSCULAS | `estado varchar(20) check (estado in ('EN_ESPERA', 'EN_JUEGO'))` |
| Texto con límite | `varchar(n)` con el límite de la regla de producto | `alias varchar(20)` |
| Unicidad sin distinguir mayúsculas | Índice único sobre `lower(columna)` | `create unique index on cuenta (lower(alias))` |
| Datos de una cuenta | `on delete cascade` desde la cuenta, para que borrarla borre todo lo suyo | `franja_horario.cuenta_id` |

- **Ninguna tabla guarda posiciones.** La API no acepta coordenadas (definición de hecho).
- **Eventos de partida** (tabla `evento`, tarea 1.1): no se modifican nunca. Identifican a los jugadores por el id de su inscripción (`inscripcion_id`), nunca por el de la cuenta, y no copian el alias ni el email. Al borrar una cuenta, sus inscripciones en partidas `EN_JUEGO` se quedan con `cuenta_id` a `null` y las de cualquier otro estado se borran (R9-7, ver «Inscripción» en `modelo-datos.md`): los eventos siguen intactos, nada en ellos identifica a esa persona y la app muestra «cuenta eliminada».

## Migraciones

- Con **Flyway**, en `backend/src/main/resources/db/migration/`. JPA queda en `ddl-auto: validate`: la estructura solo la cambian las migraciones.
- Nombre `V<AAAAMMDDHHMM>__<descripcion>.sql`, por ejemplo `V202610141030__cuenta.sql`. La fecha como versión evita que dos personas creen a la vez la misma `V2`.
- **La fecha es la del día en que se abre la *pull request* en el repositorio de clase**, no la del día en que se escribe: al abrirla, se renombra la migración. Flyway no aplica una migración más antigua que otra ya aplicada, así que una escrita por adelantado con la fecha de hoy rompería el arranque del servidor cuando llegue después que otras.
- Una migración por tarea que crea o cambia tablas, en la misma *pull request* que el código que las usa.
- **Una migración que ya está en `main` no se edita nunca.** Para corregirla se añade otra.
- **Una migración tiene que coincidir con [`modelo-datos.md`](modelo-datos.md).** Si hace falta cambiar el modelo, primero se cambia el documento en la misma *pull request*. Imad revisa y aprueba toda *pull request* que toque migraciones o el modelo, salvo las suyas, que aprueba el responsable de la zona (`.github/CODEOWNERS`).

## API REST

| Elemento | Regla | Ejemplo |
|--|--|--|
| Prefijo | `/api/v1` | `/api/v1/cuentas` |
| Recursos | Sustantivo en plural; lo del usuario autenticado cuelga de `yo` | `/api/v1/cuentas/yo/horario` |
| Campos JSON | `camelCase` | `horaInicio`, `tokenAcceso` |
| Instantes en JSON | ISO 8601 en UTC | `"2026-10-09T08:00:00Z"` |
| Horas en JSON | `HH:mm` | `"08:30"` |
| Identificadores en JSON | UUID como texto | `"3f2b…"` |
| Errores | `application/problem+json` (RFC 9457, `ProblemDetail` de Spring) con el campo `codigo` | `{"status": 409, "codigo": "ALIAS_OCUPADO", …}` |
| Autenticación | Cabecera `Authorization: Bearer <tokenAcceso>` | |

- **Sesión:** el inicio de sesión devuelve un token de acceso JWT de 15 minutos y un token de refresco de 30 días. La app guarda el de refresco en el almacenamiento seguro del móvil (SecureStore) y con él pide un par nuevo; el de refresco usado deja de valer. El servidor guarda solo el hash de cada token de refresco, para poder revocarlos al cerrar sesión o al borrar la cuenta.
- **Contrato primero:** `docs/diseno/api/openapi.yaml` se escribe antes que el código y manda. Servidor y app se programan contra él. Al cerrar cada PBI se comprueba que el OpenAPI que genera springdoc coincide con este fichero.
