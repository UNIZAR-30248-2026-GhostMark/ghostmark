# Documentación de GhostMark

Aquí solo se guarda lo que GitHub no registra por sí mismo. La pila, las tareas, el tablero, los commits y las *pull requests* ya quedan en GitHub con su historial, así que no se copian aquí.

| Qué | Dónde |
|---|---|
| Pila del producto vigente | **GitHub Issues**, etiqueta `PBI`, ordenada por el campo *Prioridad* del Project |
| Pila del sprint: objetivo, capacidad, PBI y tareas | **Milestone `SN`**: el objetivo y la capacidad van en su descripción; las PBI y las tareas son sus issues |
| Definición de hecho | **`.github/pull_request_template.md`**. Cada cambio tras una retrospectiva es un commit en esa plantilla, con el motivo en el mensaje |
| Tablero y *burn-up* | GitHub Project y sus Insights |
| Historial del trabajo | Commits, *pull requests* e Insights del repositorio |
| Instantáneas de la pila | `pilaDeProducto/instantaneas/` |
| *Burndown*, revisión y retrospectiva de cada sprint | `sprints/SN.md` |
| Gantt planificado y real | `gestion/calendario-y-gantt.md` |
| Arquitectura y decisiones técnicas | `diseno/` |
| Manual de usuario | `manual-usuario/` |
| Memoria del proyecto, por capítulos | `memoria/` |

`pilaDeProducto/pantallas/` guarda las pantallas del prototipo que enlazan los issues de las PBI más prioritarias.

## Qué hay que hacer en cada sprint

1. **Justo antes de planificar el sprint N:** exportar la pila de GitHub a `pilaDeProducto/instantaneas/SN-antes-de-planificar.md` con el orden, la talla y los criterios de cada PBI. Una vez guardada, no se vuelve a tocar. Es la pila del capítulo N.1 de la memoria, que tiene que mostrarla tal como estaba en ese momento y no puede limitarse a enlazar GitHub.
2. **Al planificar:** poner el objetivo y la capacidad en la descripción del milestone `SN`, y asignarle las PBI elegidas y sus tareas.
3. **Durante el sprint:** en cada reunión diaria, apuntar en `sprints/SN.md` la suma de las horas restantes de las tareas. De ahí sale el *burndown*.
4. **Al cerrar:** apuntar en `sprints/SN.md` lo que salga de la revisión (comentarios recibidos) y de la retrospectiva (votación y acciones de mejora medibles).

## Convenciones

- Nadie sube cambios directamente a `main`. Se trabaja en ramas `feature/<n.º de issue>-<descripción>`, o `docs/<tema>` para la documentación, y se integran por *pull request* con la aprobación de otra persona del equipo.
- Los mensajes de commit citan el issue: `… (#14)`.
- Las tareas se titulan «Tarea de #N: …». Llevan el mismo milestone que su PBI y se añaden como *sub-issue* de ella.
- En la hoja de esfuerzos, la columna Tarea lleva el número del issue.
- Fechas en formato AAAA-MM-DD. Nombres de fichero en minúsculas y sin tildes.
