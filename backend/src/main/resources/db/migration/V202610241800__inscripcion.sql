-- Tarea 3.2 (#54): inscripción y copia independiente del horario por partida.
create table inscripcion (
    id uuid primary key,
    partida_id uuid not null references partida(id) on delete cascade,
    cuenta_id uuid references cuenta(id) on delete set null,
    normas_aceptadas_en timestamptz not null,
    inscrita_en timestamptz not null,
    unique (partida_id, cuenta_id)
);

create index on inscripcion (partida_id);
create index on inscripcion (cuenta_id);

create table franja_horario_inscripcion (
    inscripcion_id uuid not null references inscripcion(id) on delete cascade,
    dia smallint not null check (dia between 1 and 5),
    hora_inicio time not null check (
        hora_inicio between '08:00' and '20:30'
        and extract(minute from hora_inicio) in (0, 30)
        and extract(second from hora_inicio) = 0
    ),
    primary key (inscripcion_id, dia, hora_inicio)
);

-- Las partidas cerradas ya no conservan datos individuales (PBI 8).
-- El bloqueo también protege la secuencia frente a escritores durante la migración.
do $$
begin
    perform id from partida order by id for update;

    insert into inscripcion (id, partida_id, cuenta_id, normas_aceptadas_en, inscrita_en)
        select gen_random_uuid(), id, master_id, creada_en, creada_en
        from partida where master_juega and master_id is not null and estado <> 'CERRADA';

    insert into franja_horario_inscripcion (inscripcion_id, dia, hora_inicio)
        select i.id, h.dia, h.hora_inicio from inscripcion i
        join franja_horario h on h.cuenta_id = i.cuenta_id;

    insert into evento (id, partida_id, secuencia, tipo, datos, ocurrido_en)
        select gen_random_uuid(), i.partida_id,
            coalesce((select max(e.secuencia) from evento e
                where e.partida_id = i.partida_id), 0) + 1,
            'JUGADOR_INSCRITO', jsonb_build_object('inscripcionId', i.id), i.inscrita_en
        from inscripcion i;
end $$;
