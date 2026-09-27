<!-- Instantánea congelada: no se edita. -->
> **Instantánea 00 · Pila del producto inicial** tal como se entregó en el informe de la fase previa (entrega del 4 de octubre de 2026). Copia literal del apartado 5. Es la pila de partida del capítulo 2.1 de la memoria salvo que se refine antes de planificar el sprint 1 (entonces manda `S1-antes-de-planificar.md`).

# 5. Pila del producto inicial

Estas son las funcionalidades de GhostMark, de más a menos prioritaria. Las 16 primeras forman la primera versión, y las 10 últimas, las dos siguientes: Ghostmark Social (L2) y Ghostmark Organizations (L3). La talla es relativa: cualquier S cuesta menos que cualquier M, y así hasta XL. Las de L2 y L3 llevan XXL porque son mayores que cualquier XL y todavía no se han desglosado. Las de la primera versión llevan criterios de satisfacción, con la forma «estará cumplido cuando se pueda hacer X y se verifique que Z». Las cinco primeras tienen además una ficha con aclaraciones y pantallas del prototipo, y el resto se resume en la tabla final. Los requisitos no funcionales están en la definición de hecho, no en la pila.

## Prioridad y tallas

| N.º | Funcionalidad | Talla | Detalle |
|--|--------------------------------|--|-----------|
| 1 | Cuenta del jugador (crear cuenta e inicio de sesión) | M | Detallado, sprint 1 |
| 2 | Crear y configurar una partida | M | Detallado, sprint 1 |
| 3 | Inscribirse en una partida | M | Detallado, sprint 1 |
| 4 | Arrancar la partida y generar la cadena de objetivos | L | Detallado, sprint 2 |
| 5 | Misiones: biblioteca, IA y aprobación del máster | L | Detallado, sprint 2 |
| 6 | Código de vida y eliminación | XL | Tabla final |
| 7 | Cierre de partida, ganador y empate | M | Tabla final |
| 8 | Horarios seguros | M | Tabla final |
| 9 | Zonas seguras | L | Tabla final |
| 10 | Disputas y anulación | XL | Tabla final |
| 11 | Abandonar, expulsar y reportar | M | Tabla final |
| 12 | Avisos al móvil | M | Tabla final |
| 13 | Ghostmark Wrapped y borrado de datos al cerrar | M | Tabla final |
| 14 | Amigos e invitaciones directas | M | Tabla final |
| 15 | Entradas tardías | S | Tabla final |
| 16 | Importar el horario lectivo desde el calendario | S | Tabla final |
| 17 | Modo por equipos | XXL | L2 |
| 18 | Purga final | XXL | L2 |
| 19 | Partida relámpago | XXL | L2 |
| 20 | Co-másters | XXL | L2 |
| 21 | Perfiles de organización | XXL | L3 |
| 22 | Plantillas de partida | XXL | L3 |
| 23 | Panel de estadísticas | XXL | L3 |
| 24 | Eventos de varias partidas | XXL | L3 |
| 25 | Validación reforzada | XXL | L3 |
| 26 | Premios | XXL | L3 |

## Fichas de las cinco funcionalidades más prioritarias

### 1. Cuenta del jugador (crear cuenta e inicio de sesión) · talla M · sprint 1

Se crea una sola vez y sirve para todas las partidas. La cuenta guarda solo el alias, los datos de acceso (email y contraseña, o Google), el horario lectivo y la lista de amigos, sin nombre real ni documentos. Mientras un jugador está en su horario lectivo no puede eliminar ni ser eliminado, y ese horario se congela al arrancar cada partida. El permiso de ubicación y el aviso de disponibilidad del objetivo se aceptan una sola vez, al crear la cuenta.

| N.º | Estará cumplido cuando… |
|--|--------------------------------------------------------|
| 1 | se pueda crear la cuenta (alias, email y contraseña, o Google) e iniciar sesión, y se verifique que con datos incorrectos no entra |
| 2 | se pueda configurar el horario lectivo y aceptar el permiso de ubicación y el aviso de disponibilidad, y se verifique que se piden una sola vez y que la cuenta solo guarda alias, datos de acceso, horario y amigos |
| 3 | se pueda borrar la cuenta y todos sus datos en cualquier momento, y se verifique que ya no se puede iniciar sesión y el alias desaparece |

<div><img src="../pantallas/f1-a1.png" alt="Alias y acceso" width="160"><img src="../pantallas/f1-a2.png" alt="Horario lectivo" width="160"><img src="../pantallas/f1-a3.png" alt="Permiso de ubicación" width="160"></div>

### 2. Crear y configurar una partida · talla M · sprint 1

El máster define la partida y la comparte, y a partir de ahí los demás se inscriben. Puede jugar o solo arbitrar: si juega, la aplicación le oculta todo lo que no le corresponde como jugador. Los pasos 3 (misiones) y 4 (zonas seguras) del asistente son las funcionalidades 5 y 9. La invitación directa a amigos llega con la 14.

| N.º | Estará cumplido cuando… |
|--|--------------------------------------------------------|
| 1 | el máster pueda crear una partida (nombre, fechas, tipo de misiones, plazo de disputas, entradas tardías, si juega), y se verifique que se guarda con esos valores y que se rechaza un fin anterior al inicio |
| 2 | obtenga un QR y un enlace para compartirla, y se verifique que el enlace abre esa partida |

<div><img src="../pantallas/f2-e1.png" alt="Datos" width="160"><img src="../pantallas/f2-e2.png" alt="Reglas" width="160"><img src="../pantallas/f2-e5.png" alt="Compartir" width="160"></div>

### 3. Inscribirse en una partida · talla M · sprint 1

Un jugador puede estar en varias partidas a la vez, cada una independiente. Al entrar acepta las normas de seguridad de esa partida: misiones sin contacto físico, sin objetos que simulen armas, zonas y horarios seguros, y libertad para abandonar. El máster ve quién se ha apuntado.

| N.º | Estará cumplido cuando… |
|--|--------------------------------------------------------|
| 1 | un jugador pueda entrar por QR o enlace, y se verifique que aparece en la lista de inscritos del máster |
| 2 | acepte las normas de la partida, y se verifique que sin aceptarlas no queda inscrito |
| 3 | pueda revisar su horario lectivo hasta que arranque el juego, y se verifique que después no cambia en esa partida |
| 4 | pueda estar en varias partidas a la vez, y se verifique que cada una es independiente |

<div><img src="../pantallas/f3-d1.png" alt="Escáner de QR" width="160"><img src="../pantallas/f3-d2.png" alt="Normas" width="160"><img src="../pantallas/f3-d3.png" alt="Sala de espera" width="160"></div>

### 4. Arrancar la partida y generar la cadena de objetivos · talla L · sprint 2

Hacen falta al menos 3 jugadores; si no se llega, la partida se cancela y se avisa a los inscritos. El máster no sabe quién tiene a quién: si juega, solo ve su propio objetivo y su propia misión. La biblioteca curada se carga con esta funcionalidad, porque cada jugador recibe su misión al arrancar. La 5 añade la IA y la aprobación del máster, y las dos van en el mismo sprint.

| N.º | Estará cumplido cuando… |
|--|--------------------------------------------------------|
| 1 | la partida arranque sola en la fecha de inicio o antes si el máster la adelanta, y se verifique que pasa a estar en juego |
| 2 | con al menos 3 jugadores se forme un único ciclo cerrado, y se verifique con 3, 4 y 10 jugadores que cada uno tiene un objetivo, es objetivo de otro y no se persigue a sí mismo |
| 3 | cada jugador vea solo su objetivo y su misión, y se verifique que nadie ve los ajenos, tampoco el máster si juega |
| 4 | con menos de 3 inscritos la partida se cancele, y se verifique que no llega a arrancar |

<div><img src="../pantallas/f4-f1.png" alt="Carta boca abajo" width="160"><img src="../pantallas/f4-f2.png" alt="Carta girada" width="160"></div>

### 5. Misiones: biblioteca, IA y aprobación del máster · talla L · sprint 2

Las misiones son siempre sin contacto físico. Salen de la biblioteca curada, ya revisada, o de una IA a partir de un contexto que da el máster. El máster aprueba siempre las de IA, que antes pasan por una validación de formato y un filtro de categorías prohibidas. Si la IA falla, la biblioteca cubre el hueco.

| N.º | Estará cumplido cuando… |
|--|--------------------------------------------------------|
| 1 | cada jugador reciba al arrancar una misión de la biblioteca curada, y se verifique que ninguna implica contacto físico ni objetos que simulen armas |
| 2 | el máster pida misiones a la IA con un contexto (por ejemplo «campus») y las apruebe o rechace una a una, y se verifique que ninguna entra en juego sin su aprobación |
| 3 | se verifique que una misión de IA con formato inválido o de categoría prohibida no llega al máster |
| 4 | si la IA falla, el máster pueda seguir con misiones de la biblioteca, y se verifique que la partida no se bloquea |

<div><img src="../pantallas/f5-e3a.png" alt="Misiones con IA" width="160"><img src="../pantallas/f5-e3b.png" alt="Aprobar misiones" width="160"></div>

## Resto de la pila

| N.º | Funcionalidad | Estará cumplido cuando… |
|--|-------|--------------------------------------------|
| 6 | Código de vida y eliminación | el cazador introduzca el código de su víctima, y se verifique que ella queda eliminada y que él hereda su objetivo y su misión |
| 7 | Cierre de partida, ganador y empate | la partida termine por último superviviente, por fecha de fin o por decisión del máster, y se verifique que se declara el ganador o el empate que corresponde |
| 8 | Horarios seguros | un jugador esté en su horario lectivo, y se verifique que no puede eliminar ni ser eliminado |
| 9 | Zonas seguras | el máster dibuje zonas seguras en el mapa, y se verifique que al introducir un código solo se guarda si el cazador estaba dentro o fuera, nunca su posición |
| 10 | Disputas y anulación | la víctima dispute una eliminación en los 10 minutos siguientes, y se verifique que, si se le da la razón, revive y la cadena de objetivos se rehace |
| 11 | Abandonar, expulsar y reportar | un jugador pueda abandonar o reportar y el máster expulsar, y se verifique que, si alguien sale, su cazador hereda su objetivo y su misión |
| 12 | Avisos al móvil | cambie algo en la partida, y se verifique que el aviso llega al momento y solo a quien toca |
| 13 | Ghostmark Wrapped y borrado de datos al cerrar | al cerrar la partida cada jugador vea y pueda compartir su Wrapped, y se verifique que de la partida solo queda el resumen agregado |
| 14 | Amigos e invitaciones directas | un jugador añada amigos por su alias y el máster los invite a una partida, y se verifique que la invitación les llega en la aplicación |
| 15 | Entradas tardías | un jugador entre tarde si el máster lo permite, y se verifique que la cadena sigue cerrada |
| 16 | Importar el horario lectivo desde el calendario | un jugador importe su horario desde un fichero .ics, y se verifique que sus clases aparecen en la rejilla y puede corregirlas |
| 17 | Modo por equipos (L2) | se pueda jugar una partida por equipos |
| 18 | Purga final (L2) | una partida con fecha límite pueda terminar con un ganador gracias a una purga final |
| 19 | Partida relámpago (L2) | se pueda jugar una partida corta, pensada para una sola tarde |
| 20 | Co-másters (L2) | varios másters puedan repartirse la gestión de una partida grande |
| 21 | Perfiles de organización (L3) | una organización pueda gestionar sus partidas desde un perfil propio |
| 22 | Plantillas de partida (L3) | una organización pueda reutilizar la configuración de sus partidas |
| 23 | Panel de estadísticas (L3) | una organización pueda consultar estadísticas agregadas de sus partidas |
| 24 | Eventos de varias partidas (L3) | una organización pueda agrupar varias partidas en un mismo evento |
| 25 | Validación reforzada (L3) | las eliminaciones se validen con más garantías frente a las trampas |
| 26 | Premios (L3) | una organización pueda ofrecer premios con reglas definidas de antemano |
