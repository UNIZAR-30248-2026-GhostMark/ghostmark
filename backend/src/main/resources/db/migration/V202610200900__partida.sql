-- Tarea 2.2 (#47): partida (PBI 2, #3).
-- Convenciones docs/diseno/convenciones-modelo-datos.md y modelo docs/diseno/modelo-datos.md.
-- Fecha 20261020 = dia de subida al repositorio de clase (calendario S1).

create table partida (
    id uuid primary key,
    nombre varchar(40) not null check (char_length(nombre) >= 3),
    master_id uuid references cuenta(id) on delete set null,
    master_juega boolean not null,
    inicio_en timestamptz not null,
    fin_en timestamptz not null check (fin_en > inicio_en),
    tipo_misiones varchar(10) not null check (tipo_misiones in ('BIBLIOTECA', 'IA')),
    plazo_disputa_horas smallint not null check (plazo_disputa_horas in (2, 6, 12, 24)),
    entradas_tardias boolean not null,
    codigo_invitacion char(8) not null unique check (codigo_invitacion ~ '^[2-9A-HJKMNP-Z]{8}$'),
    estado varchar(20) not null default 'EN_INSCRIPCION'
        check (estado in ('EN_INSCRIPCION', 'EN_JUEGO', 'CANCELADA', 'CERRADA')),
    creada_en timestamptz not null,
    actualizada_en timestamptz not null
);

create index on partida (master_id);

alter table evento add foreign key (partida_id) references partida(id) on delete cascade;
