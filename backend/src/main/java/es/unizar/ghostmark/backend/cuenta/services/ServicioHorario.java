package es.unizar.ghostmark.backend.cuenta.services;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.cuenta.dto.Horario;
import es.unizar.ghostmark.backend.cuenta.models.FranjaHorario;
import es.unizar.ghostmark.backend.cuenta.repositories.FranjaHorarioRepository;
import es.unizar.ghostmark.backend.sesion.DatosNoValidosException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioHorario {

    private static final String HORA_VALIDA = "^(0[89]|1[0-9]|20):(00|30)$";

    private final FranjaHorarioRepository franjas;
    private final CuentaRepository cuentas;

    public ServicioHorario(
            FranjaHorarioRepository franjas,
            CuentaRepository cuentas) {
        this.franjas = franjas;
        this.cuentas = cuentas;
    }

    // Consulta las franjas del usuario y devuelve el horario ordenado por día y hora
    @Transactional(readOnly = true)
    public Horario obtener(UUID cuentaId) {
        comprobarCuenta(cuentaId);
        return convertir(franjas.buscarPorCuentaId(cuentaId));
    }

    // Borra las franjas del usuario y guarda las nuevas franjas,
    // devolviendo el horario ordenado por día y hora.
    // Si alguna es incorrecta, no se guarda ninguna y se devuelve un error
    @Transactional
    public Horario guardar(UUID cuentaId, Horario horario) {
        validar(horario);
        comprobarCuenta(cuentaId);

        List<FranjaHorario> anteriores = franjas.buscarPorCuentaId(cuentaId);
        franjas.deleteAll(anteriores);
        franjas.flush();

        List<FranjaHorario> nuevas = new ArrayList<>();
        for (Horario.Franja franja : horario.franjas()) {
            nuevas.add(
                new FranjaHorario(
                    cuentaId,
                    franja.dia().shortValue(),
                    LocalTime.parse(franja.horaInicio())));
        }

        franjas.saveAll(nuevas);
        franjas.flush();

        return convertir(franjas.buscarPorCuentaId(cuentaId));
    }

    private void comprobarCuenta(UUID cuentaId) {
        if (!cuentas.existsById(cuentaId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "La cuenta no está disponible");
        }
    }

    private Horario convertir(List<FranjaHorario> entidades) {
        List<Horario.Franja> resultado = entidades.stream()
                .map(franja -> new Horario.Franja(
                        (int) franja.getId().getDia(),
                        franja.getId().getHoraInicio().toString()))
                .toList();

        return new Horario(resultado);
    }

    private void validar(Horario horario) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (horario == null || horario.franjas() == null) {
            errores.put("franjas", "El horario debe contener una lista de franjas");
            throw new DatosNoValidosException(errores);
        }

        if (horario.franjas().size() > 130) {
            errores.put("franjas", "No puede haber más de 130 franjas");
        }

        Set<Horario.Franja> encontradas = new HashSet<>();

        for (int i = 0; i < horario.franjas().size(); i++) {
            Horario.Franja franja = horario.franjas().get(i);
            String campo = "franjas[" + i + "]";

            if (franja == null) {
                errores.put(campo, "La franja no puede ser nula");
                continue;
            }

            if (franja.dia() == null
                    || franja.dia() < 1
                    || franja.dia() > 5) {
                errores.put(campo + ".dia", "El día debe estar entre 1 y 5");
            }

            if (franja.horaInicio() == null || !franja.horaInicio().matches(HORA_VALIDA)) {
                errores.put(
                        campo + ".horaInicio",
                        "La hora debe estar entre 08:00 y 20:30, cada 30 minutos");
            }

            if (!encontradas.add(franja)) {
                errores.put(campo, "La franja está duplicada");
            }
        }

        if (!errores.isEmpty()) {
            throw new DatosNoValidosException(errores);
        }
    }
}
