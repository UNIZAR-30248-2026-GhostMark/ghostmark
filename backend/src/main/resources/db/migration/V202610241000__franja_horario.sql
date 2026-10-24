-- Tarea 1.7 (#40): horario lectivo del usuario.
-- Una fila representa una franja de 30 minutos.

create table franja_horario (
    cuenta_id uuid not null
        references cuenta(id) on delete cascade,

    dia smallint not null
        check (dia between 1 and 5),

    hora_inicio time not null
        check (
            hora_inicio between '08:00' and '20:30'
            and extract(minute from hora_inicio) in (0, 30)
            and extract(second from hora_inicio) = 0
        ),

    primary key (cuenta_id, dia, hora_inicio)
);