-- Tarea 1.6 (#39): cuenta y sesiones (PBI 1, #2).
-- Convenciones docs/diseno/convenciones-modelo-datos.md y modelo docs/diseno/modelo-datos.md.
-- Fecha 20261014 = dia de subida al repositorio de clase (calendario S1).
-- El horario lectivo (franja_horario) llega con la tarea 1.7 (#40).

create table cuenta (
    id uuid primary key,
    alias varchar(20) not null check (alias ~ '^[A-Za-z0-9_]{3,20}$'),
    email varchar(254) not null,
    hash_contrasena varchar(60) not null,
    ubicacion_aceptada_en timestamptz,
    aviso_disponibilidad_aceptado_en timestamptz,
    creada_en timestamptz not null,
    actualizada_en timestamptz not null
);

create unique index on cuenta (lower(alias));
create unique index on cuenta (lower(email));

create table token_refresco (
    id uuid primary key,
    cuenta_id uuid not null references cuenta(id) on delete cascade,
    hash_token varchar(64) not null unique,
    caduca_en timestamptz not null,
    revocado_en timestamptz,
    creado_en timestamptz not null
);

create index on token_refresco (cuenta_id);
