-- Tarea 1.4 (#37): activa PostGIS para que cualquier base de datos nueva
-- (por ejemplo, al recrear la de Render) quede igual sin pasos a mano.
-- En local la imagen postgis/postgis ya la trae: el if not exists no hace nada.
create extension if not exists postgis;