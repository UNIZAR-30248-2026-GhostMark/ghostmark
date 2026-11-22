create table codigo_emitido (
    id uuid primary key,
    partida_id uuid not null references partida(id) on delete cascade,
    inscripcion_id uuid not null references inscripcion(id) on delete cascade,
    codigo char(6) not null check (codigo ~ '^[0-9]{6}$'),
    emitido_en timestamptz not null,
    caduca_en timestamptz not null check (caduca_en > emitido_en)
);

create index on codigo_emitido (inscripcion_id, caduca_en);
