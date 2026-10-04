-- Tarea 1.1 (#34): tabla de eventos de partida (PBI 1, #2).
-- Convenciones docs/diseno/convenciones-modelo-datos.md y modelo docs/diseno/modelo-datos.md.
-- Fecha 20261009 = dia de subida al repositorio de clase (calendario S1), no del dia en que se escribe.
-- Los eventos no se modifican; identifican jugadores por inscripcion_id, nunca por cuenta.

create table evento (
    id uuid primary key,
    partida_id uuid not null,
    secuencia bigint not null,
    tipo varchar(40) not null,
    datos jsonb not null,
    ocurrido_en timestamptz not null,
    unique (partida_id, secuencia)
);

-- La clave ajena a partida(id) on delete cascade llega con la tarea 2.1 (#46),
-- que crea la tabla partida (dia 13 oct, posterior a esta migracion del dia 9):
-- alter table evento add foreign key (partida_id) references partida(id) on delete cascade;

create index on evento (partida_id);
