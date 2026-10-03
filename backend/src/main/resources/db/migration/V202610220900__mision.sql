-- Tarea 5.1 (#67): biblioteca curada inicial de misiones (PBI 5, #31).
-- Revisadas por el Dueño del Producto el 2026-10-03: ninguna implica contacto físico,
-- objetos que simulen armas, alcohol, riesgo físico, contenido sexual, entrar en un sitio
-- sin permiso ni ridiculizar a nadie (Descripción de producto, apartado 7).

create table mision (
    id uuid primary key,
    texto varchar(200) not null unique,
    activa boolean not null default true,
    creada_en timestamptz not null
);

insert into mision (id, texto, activa, creada_en) values
    (gen_random_uuid(), 'Que te preste un bolígrafo', true, now()),
    (gen_random_uuid(), 'Que diga la palabra «compilador»', true, now()),
    (gen_random_uuid(), 'Que te diga la hora', true, now()),
    (gen_random_uuid(), 'Que se haga un selfie contigo', true, now()),
    (gen_random_uuid(), 'Que te diga cuál es su comida favorita', true, now()),
    (gen_random_uuid(), 'Que te diga cuál es su asignatura favorita', true, now()),
    (gen_random_uuid(), 'Que te recomiende una canción', true, now()),
    (gen_random_uuid(), 'Que te recomiende una serie o una película', true, now()),
    (gen_random_uuid(), 'Que diga la palabra «algoritmo»', true, now()),
    (gen_random_uuid(), 'Que te pregunte a ti qué hora es', true, now()),
    (gen_random_uuid(), 'Que te diga cuál es su número de la suerte', true, now()),
    (gen_random_uuid(), 'Que te haga un dibujo en un papel', true, now()),
    (gen_random_uuid(), 'Que te diga cuál es el último libro que ha leído', true, now()),
    (gen_random_uuid(), 'Que diga la palabra «fantasma»', true, now()),
    (gen_random_uuid(), 'Que te recomiende un sitio para comer cerca del campus', true, now()),
    (gen_random_uuid(), 'Que te cuente un chiste', true, now()),
    (gen_random_uuid(), 'Que te diga qué emoji usa más', true, now()),
    (gen_random_uuid(), 'Que te diga qué tiempo va a hacer mañana', true, now()),
    (gen_random_uuid(), 'Que te diga cuál es su videojuego favorito', true, now()),
    (gen_random_uuid(), 'Que te preste cualquier objeto que no sea su móvil', true, now()),
    (gen_random_uuid(), 'Que diga una palabra que empiece por «z»', true, now()),
    (gen_random_uuid(), 'Que te ofrezca un caramelo o un chicle', true, now()),
    (gen_random_uuid(), 'Que te diga en qué ciudad nació', true, now()),
    (gen_random_uuid(), 'Que te enseñe a decir una palabra en otro idioma', true, now());
