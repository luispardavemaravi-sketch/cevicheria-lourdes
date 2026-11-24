package pe.cevicherialourdes.carta.domain.port.out;

import java.util.List;

import pe.cevicherialourdes.carta.domain.model.Acompanamiento;

public interface AcompanamientoRepositorio {

    /**
     * Busca cada nombre en el catalogo sin importar mayusculas y crea los que no existen.
     * Devuelve uno por nombre, en el mismo orden. Los existentes conservan su nombre original.
     */
    List<Acompanamiento> obtenerOCrear(List<String> nombres);
}
