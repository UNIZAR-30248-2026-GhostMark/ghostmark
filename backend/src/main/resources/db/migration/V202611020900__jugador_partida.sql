-- Tarea 7a.2 (#193): estado del jugador y secreto del código de vida
create table jugador_partida (
    inscripcion_id uuid primary key references inscripcion(id) on delete cascade,
    partida_id uuid not null references partida(id) on delete cascade,
    estado varchar(20) not null check (estado in ('VIVO', 'ELIMINADO')),
    objetivo_id uuid references inscripcion(id),
    mision_id uuid references mision(id),
    eliminaciones smallint not null default 0,
    secreto_codigo bytea not null check (octet_length(secreto_codigo) = 20),
    intentos_fallidos smallint not null default 0 check (intentos_fallidos between 0 and 3),
    bloqueado_hasta timestamptz,
    eliminado_en timestamptz
);

create index on jugador_partida (partida_id);
create index on jugador_partida (objetivo_id);
create index on jugador_partida (mision_id);

-- Relleno de las partidas que ya están en juego, a partir de CICLO_FORMADO y MISIONES_REPARTIDAS
-- (incluye las inscripciones de cuentas borradas)
do $$
begin
    perform id from partida order by id for update;

    insert into jugador_partida (inscripcion_id, partida_id, estado, objetivo_id,
                                mision_id, secreto_codigo)
        with ciclo as (
            select e.partida_id, jugador.id::uuid as id,
                   lead(jugador.id, 1, e.datos->'orden'->>0)
                       over (partition by e.partida_id order by jugador.pos)::uuid as objetivo_id
            from evento e
            join partida p on p.id = e.partida_id and p.estado = 'EN_JUEGO'
            cross join lateral jsonb_array_elements_text(e.datos->'orden')
                with ordinality as jugador(id, pos)
            where e.tipo = 'CICLO_FORMADO'
        )
        select ciclo.id, ciclo.partida_id, 'VIVO', ciclo.objetivo_id,
               (a.valor->>'misionId')::uuid,
               substring(uuid_send(gen_random_uuid()) || uuid_send(gen_random_uuid()) from 1 for 20)
        from ciclo
        join evento e on e.partida_id = ciclo.partida_id and e.tipo = 'MISIONES_REPARTIDAS'
        cross join lateral jsonb_array_elements(e.datos->'asignaciones') as a(valor)
        where (a.valor->>'inscripcionId')::uuid = ciclo.id;
end $$;
