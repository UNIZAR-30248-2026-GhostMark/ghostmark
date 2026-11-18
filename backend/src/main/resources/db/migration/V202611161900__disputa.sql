-- Tarea 11a.1 (#290): disputas sobre una eliminación o por negativa a enseñar el código (PBI 11a y 11b)
-- Copia del estado de cada disputa; sus eventos son DISPUTA_ABIERTA, DISPUTA_RESPONDIDA,
-- DISPUTA_ARBITRO_SORTEADO y DISPUTA_RESUELTA.
create table disputa (
    id uuid primary key,
    partida_id uuid not null references partida(id) on delete cascade,
    tipo varchar(20) not null check (tipo in ('ELIMINACION', 'NEGATIVA')),
    cazador_id uuid not null references inscripcion(id),
    victima_id uuid not null references inscripcion(id),
    evento_eliminacion_id uuid unique references evento(id) on delete cascade,
    texto_apertura varchar(280) not null check (char_length(btrim(texto_apertura)) between 1 and 280),
    texto_respuesta varchar(280) check (char_length(btrim(texto_respuesta)) between 1 and 280),
    arbitro_id uuid references inscripcion(id),
    plazo_hasta timestamptz not null,
    estado varchar(20) not null check (estado in ('ABIERTA', 'RESUELTA', 'SIN_OBJETO')),
    razon varchar(10) check (razon in ('CAZADOR', 'VICTIMA')),
    resuelta_por varchar(10) check (resuelta_por in ('MASTER', 'SORTEADO', 'ZONA')),
    abierta_en timestamptz not null,
    respondida_en timestamptz,
    cerrada_en timestamptz,
    check ((tipo = 'ELIMINACION') = (evento_eliminacion_id is not null)),
    check ((estado = 'RESUELTA') = (razon is not null)),
    check ((estado = 'RESUELTA') = (resuelta_por is not null)),
    check ((estado = 'ABIERTA') = (cerrada_en is null)),
    check ((texto_respuesta is null) = (respondida_en is null)),
    check (cazador_id <> victima_id)
);

create index on disputa (partida_id);
create index on disputa (cazador_id) where estado = 'ABIERTA';
