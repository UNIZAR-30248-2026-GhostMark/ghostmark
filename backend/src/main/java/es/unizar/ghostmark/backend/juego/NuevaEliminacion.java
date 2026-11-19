package es.unizar.ghostmark.backend.juego;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class NuevaEliminacion {

    private String codigo;
    private String resultadoZona;
    private final Map<String, Object> otros = new LinkedHashMap<>();

    public NuevaEliminacion() {
    }

    public NuevaEliminacion(String codigo, String resultadoZona) {
        this.codigo = codigo;
        this.resultadoZona = resultadoZona;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getResultadoZona() {
        return resultadoZona;
    }

    public void setResultadoZona(String resultadoZona) {
        this.resultadoZona = resultadoZona;
    }

    @JsonAnySetter
    public void otroCampo(String nombre, Object valor) {
        otros.put(nombre, valor);
    }

    public Set<String> camposNoPermitidos() {
        return otros.keySet();
    }
}
