## PBI relacionada
Closes #<n> <!-- n.º issue PBI. Las tareas citan su PBI aquí -->

## Qué cambia
<!-- breve -->

## Checklist DoD — una PBI está hecha cuando no hay ningún NO. Marca [x] Sí, deja [ ] NO, o N/A.
### Calidad y pruebas
- [ ] Todas las PR de esta PBI están integradas en `main` con aprobación de otra persona
- [ ] CI en verde: compila, pasan tests, sin errores Checkstyle (servidor) ni ESLint/Prettier (móvil)
- [ ] Cobertura servidor JaCoCo >= 75% medida en CI
- [ ] Cada criterio de satisfacción tiene test de aceptación automático que pasa
- [ ] Sin defectos `bloqueante` ni `critico` abiertos de esta PBI (`leve` resto)

### NFR producto — si no aplica a esta PBI marca N/A, nunca lo borres
- [ ] Privacidad ubicación: servidor solo recibe `dentro/fuera/no concluyente`, API no acepta ni guarda coordenadas. Test lo comprueba. N/A:
- [ ] Roles: cada rol solo hace/ve lo suyo. Test por rol incl. pedir objetivo/misión/código ajenos se rechaza. N/A:
- [ ] Código vida 6 dígitos solo dueño, rota 60s, caducado se rechaza. Test. N/A:
- [ ] Borrar cuenta: desaparecen cuenta/datos, eventos quedan con `cuenta eliminada`. Test. N/A:
- [ ] Cierre: se borran eventos/objetivos/misiones/códigos, solo queda resumen agregado. Test. N/A:
- [ ] Event-sourcing: estado se reconstruye de eventos. Test reproduce y coincide. N/A:
- [ ] Fallback IA: si IA falla sigue con biblioteca. Test con servicio caído. N/A:
- [ ] Sin permiso ubicación no se puede introducir código (provisional). N/A:
- [ ] Passwords con hash y API solo HTTPS, HTTP se rechaza/redirige. Test. N/A:
- [ ] Sin claves/tokens/secretos en repo, solo en variables entorno despliegue

### Entrega
- [ ] Desplegado en prod vía Actions + móvil EAS, comprobado a mano uso normal por un miembro
- [ ] Diseño, OpenAPI y manual actualizados en la PR si cambian