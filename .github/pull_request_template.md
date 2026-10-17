<!-- Título: qué cambia, en una línea. Rama: feature/<n.º de issue>-<descripción> -->

**PBI:** #
**Tareas que cierra:** Closes #
**Revisa:** @
<!-- El responsable de la zona que toca la PR (.github/CODEOWNERS): app, Alejo; servidor, Diego; documentación e integración continua, Sergio.
     Si el autor es el responsable, revisa el segundo (app, Imad; servidor, Alejo; documentación e integración continua, Diego).
     Imad sustituye a quien no esté disponible. Plazo: dos días laborables desde que se abre la PR. -->

## Qué cambia

<!-- Dos o tres líneas. Si hay pantalla nueva o cambiada, añade una captura. -->

## Cómo probarlo

<!-- Pasos para que quien revisa lo compruebe. -->

## Definición de hecho

Quien revisa prueba el cambio con los pasos de arriba y marca cada punto. Si un punto no aplica a este cambio, se marca y se escribe «(no aplica)» al final. La PBI sigue abierta mientras quede un punto sin marcar en alguna de sus *pull requests*.

- [ ] Aprobada por el responsable de lo que toca (o su segundo, o Imad), dentro del plazo de dos días laborables; esta rama es solo de esta funcionalidad.
- [ ] Integración continua en verde: compila, pasan los tests, la cobertura del servidor (JaCoCo) llega al 75 % y no hay avisos de Checkstyle (servidor) ni errores de ESLint (móvil).
- [ ] Cada criterio de satisfacción tiene al menos un test de aceptación automático, uno por cada caso que nombra, y todos pasan.
- [ ] Los fallos encontrados en la revisión o en producción están corregidos; solo queda abierto en la pila alguno que no impida cumplir un criterio ni pierda o exponga datos.
- [ ] No se suben claves, tokens, contraseñas ni ficheros `.env`: los secretos están en variables de entorno del servicio de despliegue (el escaneo de secretos de GitHub rechaza además la subida).
- [ ] La posición del jugador no se almacena ni se registra: el servidor solo recibe dentro, fuera o no concluyente; la API no acepta coordenadas y un test lo comprueba.
- [ ] Cada rol (máster, jugador vivo, espectador, abandonado o expulsado) solo hace y ve lo suyo; un test por rol comprueba que lo demás se rechaza, incluido pedir objetivo, misión o código de vida ajenos.
- [ ] Si la PR guarda un dato nuevo de un jugador, se borra al borrar su cuenta y, en partidas en curso, su alias pasa a «cuenta eliminada»; el test de borrado de cuenta lo comprueba.
- [ ] Contraseñas con hash y API solo por HTTPS; un test verifica que la contraseña guardada no es legible y que una petición HTTP se rechaza o se redirige.
- [ ] Desplegado en producción con GitHub Actions (servidor desplegado y móvil publicado con Expo EAS) y alguien del equipo ha repetido allí, a mano, los criterios de satisfacción de la PBI.
- [ ] Diseño, API en OpenAPI y manual de usuario actualizados en lo que cambie.
