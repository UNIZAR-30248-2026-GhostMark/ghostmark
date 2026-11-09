-- Tarea 6a.2 (#207): misiones generadas con IA en la tabla mision.
alter table mision add column partida_id uuid references partida(id) on delete cascade;
alter table mision add column estado varchar(20)
    check (estado in ('PROPUESTA', 'APROBADA', 'RECHAZADA'));
alter table mision add column tanda smallint check (tanda between 1 and 5);
alter table mision add column revisada_en timestamptz;
alter table mision add check ((partida_id is null) = (estado is null));
alter table mision add check (partida_id is not null or (tanda is null and revisada_en is null));
alter table mision add check ((revisada_en is null) = (estado = 'PROPUESTA'));
create index on mision (partida_id);
alter table mision drop constraint mision_texto_key;
create unique index on mision (texto) where partida_id is null;
create unique index on mision (partida_id, lower(texto));
