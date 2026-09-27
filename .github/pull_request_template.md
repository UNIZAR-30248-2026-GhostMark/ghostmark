<!-- Título: qué cambia, en una línea. Rama: feature/<n.º de issue>-<descripción> -->

**PBI:** #
**Tareas que cierra:** Closes #

## Qué cambia

<!-- Dos o tres líneas. Si hay pantalla nueva o cambiada, añade una captura. -->

## Cómo probarlo

<!-- Pasos para que quien revisa lo compruebe. -->

## Definición de hecho

Quien revisa marca cada punto. Si un punto no aplica a este cambio, se marca y se escribe «(no aplica)» al final. La PBI sigue abierta mientras quede un punto sin marcar en alguna de sus *pull requests*.

**Calidad y pruebas**

- [ ] Todas las *pull requests* de la PBI están integradas en `main`, que está protegida y no admite subidas directas; esta rama es solo de esta funcionalidad y la aprueba al menos otra persona del equipo.
- [ ] Integración continua en verde: compila, pasan los tests, la cobertura del servidor (JaCoCo) llega al 75 % y no hay errores de Checkstyle (servidor) ni ESLint (móvil).
- [ ] Cada criterio de satisfacción tiene un test de aceptación automático que pasa, y juntos cubren el uso normal.
- [ ] No hay abierto ningún defecto bloqueante o crítico de la PBI (etiquetas `gravedad:bloqueante`, `gravedad:critico`).

**Requisitos no funcionales del producto**

- [ ] La posición del jugador no se almacena ni se registra: el servidor solo recibe dentro, fuera o no concluyente; la API no acepta coordenadas y un test lo comprueba.
- [ ] Cada rol (máster, jugador vivo, espectador, abandonado o expulsado) solo hace y ve lo suyo; un test por rol comprueba que lo demás se rechaza, incluido pedir objetivo, misión o código de vida ajenos.
- [ ] El código de vida (6 dígitos) solo lo ve su dueño y, desde que abre su pantalla, cambia cada 60 s hasta que la cierra.
- [ ] Al borrar una cuenta desaparecen sus datos y en los eventos de partidas en curso su alias pasa a «cuenta eliminada»; un test comprueba que nada la identifica.
- [ ] Al cerrar una partida se borran eventos, objetivos, misiones y códigos de vida y solo queda el resumen agregado; un test lo comprueba.
- [ ] Cada hecho de la partida se guarda como evento y el estado se reconstruye a partir de ellos; un test reproduce los eventos y compara con el estado del servidor.
- [ ] Si el servicio de IA falla o no responde, la partida sigue con misiones de la biblioteca curada; un test con el servicio simulado caído lo comprueba.
- [ ] El permiso de ubicación es obligatorio para jugar: se da al crear la cuenta y, sin él, no se puede introducir un código.
- [ ] Contraseñas con hash y API solo por HTTPS; un test verifica que la contraseña guardada no es legible y que una petición HTTP se rechaza o se redirige.
- [ ] Ninguna clave, token ni secreto en el repositorio: están en variables de entorno del servicio de despliegue.

**Entrega**

- [ ] Desplegado en producción con GitHub Actions (servidor desplegado y móvil publicado con Expo EAS) y una persona del equipo ha comprobado allí, a mano, el uso normal.
- [ ] Diseño, API en OpenAPI y manual de usuario actualizados en lo que cambie.
