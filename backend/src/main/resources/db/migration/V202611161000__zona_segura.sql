-- Tarea 10a.1 (#280): zonas seguras de la partida (PBI 10a y 10b)
-- Polígono en WGS 84 (EPSG:4326), anillo cerrado: de 3 a 20 puntos distintos son de 4 a 21 puntos.
create table zona_segura (
    id uuid primary key,
    partida_id uuid not null references partida(id) on delete cascade,
    nombre varchar(30) not null check (char_length(btrim(nombre)) between 1 and 30),
    poligono geometry(Polygon, 4326) not null
        check (ST_IsValid(poligono) and ST_NPoints(poligono) between 4 and 21),
    creada_en timestamptz not null
);

create index on zona_segura (partida_id);
create unique index on zona_segura (partida_id, lower(nombre));
