# Comentario de revisión de una *pull request*

Quien revisa deja un único comentario con este formato al aprobar o al pedir cambios («Request changes»). Así todas las revisiones dicen lo mismo en el mismo orden y queda constancia de qué se ha comprobado.

```
Probado: <qué se ha ejecutado o mirado y con qué resultado>.
No probado: <lo que no se ha podido comprobar y por qué; si no hay nada, se borra la línea>.

Antes de aprobar:
1. <cambio necesario, con el fichero y la línea o el mensaje de error>

Sugerencias:
- <mejora que no bloquea; se indica en qué tarea se puede hacer>
```

- Si no hay nada en «Antes de aprobar», se borra la sección y se aprueba.
- Si hay algo, se marca «Request changes». Cuando el autor lo corrige, quien revisa lo comprueba y aprueba con un comentario corto.
- Las casillas de la definición de hecho se marcan en la descripción de la PR, no en el comentario.

## Guardarlo como respuesta guardada

Se hace una vez por persona: GitHub → foto de perfil → **Settings** → **Saved replies** → título «Revisión de PR» → pegar el bloque de arriba → **Add saved reply**. Al revisar, el icono de la flecha curva de la caja de comentario lo inserta.
