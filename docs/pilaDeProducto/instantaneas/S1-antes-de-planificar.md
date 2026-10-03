<!-- Instantánea congelada: no se edita. -->
> **Instantánea S1 · Pila del producto antes de planificar el sprint 1**, exportada de los issues de GitHub y del campo «Prioridad» del Project el 3 de octubre de 2026, antes de la sesión de planificación del 8 de octubre. Ninguna PBI tiene aún milestone ni tareas. Es la pila de partida del capítulo 2.1 de la memoria.

# Pila del producto antes de planificar el sprint 1

## Prioridad y tallas

| Prioridad | Issue | Funcionalidad | Talla | Lanzamiento |
|--|--|--------------------------------|--|--|
| 1 | #2 | Cuenta del jugador (crear cuenta e inicio de sesión) | M | L1 |
| 2 | #3 | Crear y configurar una partida | M | L1 |
| 3 | #4 | Inscribirse en una partida | M | L1 |
| 4 | #5 | Arrancar la partida y formar la cadena de objetivos | M | L1 |
| 5 | #31 | Ver mi objetivo y mi misión | S | L1 |
| 6 | #6 | Misiones: biblioteca, IA y aprobación del máster | L | L1 |
| 7 | #7 | Código de vida y eliminación | XL | L1 |
| 8 | #8 | Cierre de partida, ganador y empate | M | L1 |
| 9 | #9 | Horarios seguros | M | L1 |
| 10 | #10 | Zonas seguras | L | L1 |
| 11 | #11 | Disputas y anulación | XL | L1 |
| 12 | #12 | Abandonar, expulsar y reportar | M | L1 |
| 13 | #13 | Avisos al móvil | M | L1 |
| 14 | #14 | Ghostmark Wrapped y borrado de datos al cerrar | M | L1 |
| 15 | #15 | Amigos e invitaciones directas | M | L1 |
| 16 | #16 | Entradas tardías | S | L1 |
| 17 | #17 | Importar el horario lectivo desde el calendario | S | L1 |
| 18 | #32 | Acceso con Google | S | L1 |
| 19 | #18 | Modo por equipos | XXL | L2 |
| 20 | #19 | Purga final | XXL | L2 |
| 21 | #20 | Partida relámpago | XXL | L2 |
| 22 | #21 | Co-másters | XXL | L2 |
| 23 | #22 | Perfiles de organización | XXL | L3 |
| 24 | #23 | Plantillas de partida | XXL | L3 |
| 25 | #24 | Panel de estadísticas | XXL | L3 |
| 26 | #25 | Eventos de varias partidas | XXL | L3 |
| 27 | #26 | Validación reforzada | XXL | L3 |
| 28 | #27 | Premios | XXL | L3 |

## Fichas

### 1. Cuenta del jugador (crear cuenta e inicio de sesión) (#2)

**Talla:** M  
**Lanzamiento:** L1  
**Prioridad inicial:** 1 de 28

#### Descripción

Se crea una sola vez y sirve para todas las partidas. La cuenta guarda solo el alias, los datos de acceso (email y contraseña), el horario lectivo y la lista de amigos, sin nombre real ni documentos. Mientras un jugador está en su horario lectivo no puede eliminar ni ser eliminado, y ese horario se congela al arrancar cada partida. El permiso de ubicación y el aviso de disponibilidad del objetivo se aceptan una sola vez, al crear la cuenta. El acceso con una cuenta de Google llega con la 18.

#### Criterios de satisfacción

1. Estará cumplido cuando se pueda crear la cuenta (alias, email y contraseña) e iniciar sesión, y se verifique que con datos incorrectos no entra.
2. Estará cumplido cuando se pueda configurar el horario lectivo y aceptar el permiso de ubicación y el aviso de disponibilidad, y se verifique que se piden una sola vez y que la cuenta solo guarda alias, datos de acceso, horario y amigos.
3. Estará cumplido cuando se pueda borrar la cuenta y todos sus datos en cualquier momento, y se verifique que ya no se puede iniciar sesión y el alias desaparece.

#### Pantallas del prototipo

<img src="../pantallas/f1-a1.png" alt="Alias y acceso" width="220"> <img src="../pantallas/f1-a2.png" alt="Horario lectivo" width="220"> <img src="../pantallas/f1-a3.png" alt="Permiso de ubicación" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 2. Crear y configurar una partida (#3)

**Talla:** M  
**Lanzamiento:** L1  
**Prioridad inicial:** 2 de 28

#### Descripción

El máster define la partida y la comparte, y a partir de ahí los demás se inscriben. Puede jugar o solo arbitrar: si juega, la aplicación le oculta todo lo que no le corresponde como jugador. Los pasos 3 (misiones) y 4 (zonas seguras) del asistente son las funcionalidades 6 y 10. La invitación directa a amigos llega con la 15.

#### Criterios de satisfacción

1. Estará cumplido cuando el máster pueda crear una partida (nombre, fechas, tipo de misiones, plazo de disputas, entradas tardías, si juega), y se verifique que se guarda con esos valores y que se rechaza un fin anterior al inicio.
2. Estará cumplido cuando obtenga un QR y un enlace para compartirla, y se verifique que el enlace abre esa partida.

#### Pantallas del prototipo

<img src="../pantallas/f2-e1.png" alt="Datos" width="220"> <img src="../pantallas/f2-e2.png" alt="Reglas" width="220"> <img src="../pantallas/f2-e5.png" alt="Compartir" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 3. Inscribirse en una partida (#4)

**Talla:** M  
**Lanzamiento:** L1  
**Prioridad inicial:** 3 de 28

#### Descripción

Un jugador puede estar en varias partidas a la vez, cada una independiente. Al entrar acepta las normas de seguridad de esa partida: misiones sin contacto físico, sin objetos que simulen armas, zonas y horarios seguros, y libertad para abandonar. El máster ve quién se ha apuntado.

#### Criterios de satisfacción

1. Estará cumplido cuando un jugador pueda entrar por QR o enlace, y se verifique que aparece en la lista de inscritos del máster.
2. Estará cumplido cuando acepte las normas de la partida, y se verifique que sin aceptarlas no queda inscrito.
3. Estará cumplido cuando pueda revisar su horario lectivo hasta que arranque el juego, y se verifique que después no cambia en esa partida.
4. Estará cumplido cuando pueda estar en varias partidas a la vez, y se verifique que cada una es independiente.

#### Pantallas del prototipo

<img src="../pantallas/f3-d1.png" alt="Escáner de QR" width="220"> <img src="../pantallas/f3-d2.png" alt="Normas" width="220"> <img src="../pantallas/f3-d3.png" alt="Sala de espera" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 4. Arrancar la partida y formar la cadena de objetivos (#5)

**Talla:** M  
**Lanzamiento:** L1  
**Prioridad inicial:** 4 de 28

#### Descripción

La cadena es un único ciclo cerrado: cada jugador persigue a otro y nadie queda fuera. Hacen falta al menos 3 jugadores; si no se llega, la partida se cancela y los inscritos lo ven al abrir la aplicación. El aviso al móvil llega con la 13.

#### Criterios de satisfacción

1. Estará cumplido cuando la partida arranque sola en la fecha de inicio o antes si el máster la adelanta, y se verifique que pasa a estar en juego.
2. Estará cumplido cuando con al menos 3 jugadores se forme un único ciclo cerrado, y se verifique con 3, 4 y 10 jugadores que cada uno tiene un objetivo, es objetivo de otro y no se persigue a sí mismo.
3. Estará cumplido cuando con menos de 3 inscritos la partida se cancele, y se verifique que no llega a arrancar.

#### Pantallas del prototipo

<img src="../pantallas/f4-arranca.png" alt="La partida arranca" width="220"> <img src="../pantallas/f4-cancelada.png" alt="Partida cancelada" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 5. Ver mi objetivo y mi misión (#31)

**Talla:** S  
**Lanzamiento:** L1  
**Prioridad inicial:** 5 de 28

#### Descripción

Al arrancar, cada jugador recibe un objetivo y una misión en una carta que solo él puede girar. El máster no sabe quién tiene a quién: si juega, solo ve su propio objetivo y su propia misión. Las misiones salen de una biblioteca curada inicial, corta y ya revisada. La 6 la amplía y añade la IA y la aprobación del máster.

#### Criterios de satisfacción

1. Estará cumplido cuando cada jugador vea solo su objetivo y su misión, y se verifique que nadie ve los ajenos, tampoco el máster si juega.
2. Estará cumplido cuando cada jugador reciba al arrancar una misión de la biblioteca curada, y se verifique que ninguna implica contacto físico ni objetos que simulen armas.

#### Pantallas del prototipo

<img src="../pantallas/f4-f1.png" alt="Carta boca abajo" width="220"> <img src="../pantallas/f4-f2.png" alt="Carta girada" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 6. Misiones: biblioteca, IA y aprobación del máster (#6)

**Talla:** L  
**Lanzamiento:** L1  
**Prioridad inicial:** 6 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando el máster pida misiones a la IA con un contexto y las apruebe o rechace una a una, y se verifique que ninguna entra en juego sin su aprobación.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 7. Código de vida y eliminación (#7)

**Talla:** XL  
**Lanzamiento:** L1  
**Prioridad inicial:** 7 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando el cazador introduzca el código de su víctima, y se verifique que ella queda eliminada y que él hereda su objetivo y su misión.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 8. Cierre de partida, ganador y empate (#8)

**Talla:** M  
**Lanzamiento:** L1  
**Prioridad inicial:** 8 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando la partida termine por último superviviente, por fecha de fin o por decisión del máster, y se verifique que se declara el ganador o el empate que corresponde.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 9. Horarios seguros (#9)

**Talla:** M  
**Lanzamiento:** L1  
**Prioridad inicial:** 9 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando un jugador esté en su horario lectivo, y se verifique que no puede eliminar ni ser eliminado.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 10. Zonas seguras (#10)

**Talla:** L  
**Lanzamiento:** L1  
**Prioridad inicial:** 10 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando el máster dibuje zonas seguras en el mapa, y se verifique que al introducir un código solo se guarda si el cazador estaba dentro o fuera, nunca su posición.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 11. Disputas y anulación (#11)

**Talla:** XL  
**Lanzamiento:** L1  
**Prioridad inicial:** 11 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando la víctima dispute una eliminación en los 10 minutos siguientes, y se verifique que, si se le da la razón, revive y la cadena de objetivos se rehace.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 12. Abandonar, expulsar y reportar (#12)

**Talla:** M  
**Lanzamiento:** L1  
**Prioridad inicial:** 12 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando un jugador pueda abandonar o reportar y el máster expulsar, y se verifique que, si alguien sale, su cazador hereda su objetivo y su misión.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 13. Avisos al móvil (#13)

**Talla:** M  
**Lanzamiento:** L1  
**Prioridad inicial:** 13 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando cambie algo en la partida, y se verifique que el aviso llega al momento y solo a quien toca.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 14. Ghostmark Wrapped y borrado de datos al cerrar (#14)

**Talla:** M  
**Lanzamiento:** L1  
**Prioridad inicial:** 14 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando al cerrar la partida cada jugador vea y pueda compartir su Wrapped, y se verifique que de la partida solo queda el resumen agregado.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 15. Amigos e invitaciones directas (#15)

**Talla:** M  
**Lanzamiento:** L1  
**Prioridad inicial:** 15 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando un jugador añada amigos por su alias y el máster los invite a una partida, y se verifique que la invitación les llega en la aplicación.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 16. Entradas tardías (#16)

**Talla:** S  
**Lanzamiento:** L1  
**Prioridad inicial:** 16 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando un jugador entre tarde si el máster lo permite, y se verifique que la cadena sigue cerrada.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 17. Importar el horario lectivo desde el calendario (#17)

**Talla:** S  
**Lanzamiento:** L1  
**Prioridad inicial:** 17 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando un jugador importe su horario desde un fichero .ics, y se verifique que sus clases aparecen en la rejilla y puede corregirlas.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 18. Acceso con Google (#32)

**Talla:** S  
**Lanzamiento:** L1  
**Prioridad inicial:** 18 de 28

#### Descripción

Se detalla en el refinamiento previo al sprint en que se planifique.

#### Criterios de satisfacción

1. Estará cumplido cuando un jugador cree su cuenta o inicie sesión con su cuenta de Google, y se verifique que no se le pide contraseña y que la cuenta guarda los mismos datos.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 19. Modo por equipos (#18)

**Talla:** XXL  
**Lanzamiento:** L2  
**Prioridad inicial:** 19 de 28

#### Descripción

Funcionalidad del lanzamiento L2. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.

#### Criterios de satisfacción

1. Estará cumplido cuando se pueda jugar una partida por equipos.

#### Tareas

Ninguna hasta que se desglose.

### 20. Purga final (#19)

**Talla:** XXL  
**Lanzamiento:** L2  
**Prioridad inicial:** 20 de 28

#### Descripción

Funcionalidad del lanzamiento L2. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.

#### Criterios de satisfacción

1. Estará cumplido cuando una partida con fecha límite pueda terminar con un ganador gracias a una purga final.

#### Tareas

Ninguna hasta que se desglose.

### 21. Partida relámpago (#20)

**Talla:** XXL  
**Lanzamiento:** L2  
**Prioridad inicial:** 21 de 28

#### Descripción

Funcionalidad del lanzamiento L2. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.

#### Criterios de satisfacción

1. Estará cumplido cuando se pueda jugar una partida corta, pensada para una sola tarde.

#### Tareas

Ninguna hasta que se desglose.

### 22. Co-másters (#21)

**Talla:** XXL  
**Lanzamiento:** L2  
**Prioridad inicial:** 22 de 28

#### Descripción

Funcionalidad del lanzamiento L2. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.

#### Criterios de satisfacción

1. Estará cumplido cuando varios másters puedan repartirse la gestión de una partida grande.

#### Tareas

Ninguna hasta que se desglose.

### 23. Perfiles de organización (#22)

**Talla:** XXL  
**Lanzamiento:** L3  
**Prioridad inicial:** 23 de 28

#### Descripción

Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.

#### Criterios de satisfacción

1. Estará cumplido cuando una organización pueda gestionar sus partidas desde un perfil propio.

#### Tareas

Ninguna hasta que se desglose.

### 24. Plantillas de partida (#23)

**Talla:** XXL  
**Lanzamiento:** L3  
**Prioridad inicial:** 24 de 28

#### Descripción

Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.

#### Criterios de satisfacción

1. Estará cumplido cuando una organización pueda reutilizar la configuración de sus partidas.

#### Tareas

Ninguna hasta que se desglose.

### 25. Panel de estadísticas (#24)

**Talla:** XXL  
**Lanzamiento:** L3  
**Prioridad inicial:** 25 de 28

#### Descripción

Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.

#### Criterios de satisfacción

1. Estará cumplido cuando una organización pueda consultar estadísticas agregadas de sus partidas.

#### Tareas

Ninguna hasta que se desglose.

### 26. Eventos de varias partidas (#25)

**Talla:** XXL  
**Lanzamiento:** L3  
**Prioridad inicial:** 26 de 28

#### Descripción

Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.

#### Criterios de satisfacción

1. Estará cumplido cuando una organización pueda agrupar varias partidas en un mismo evento.

#### Tareas

Ninguna hasta que se desglose.

### 27. Validación reforzada (#26)

**Talla:** XXL  
**Lanzamiento:** L3  
**Prioridad inicial:** 27 de 28

#### Descripción

Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.

#### Criterios de satisfacción

1. Estará cumplido cuando las eliminaciones se validen con más garantías frente a las trampas.

#### Tareas

Ninguna hasta que se desglose.

### 28. Premios (#27)

**Talla:** XXL  
**Lanzamiento:** L3  
**Prioridad inicial:** 28 de 28

#### Descripción

Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.

#### Criterios de satisfacción

1. Estará cumplido cuando una organización pueda ofrecer premios con reglas definidas de antemano.

#### Tareas

Ninguna hasta que se desglose.
