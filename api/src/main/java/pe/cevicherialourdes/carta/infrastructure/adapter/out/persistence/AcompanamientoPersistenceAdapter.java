package pe.cevicherialourdes.carta.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import pe.cevicherialourdes.carta.domain.model.Acompanamiento;
import pe.cevicherialourdes.carta.domain.port.out.AcompanamientoRepositorio;

@Component
class AcompanamientoPersistenceAdapter implements AcompanamientoRepositorio {

    private final AcompanamientoJpaRepository jpa;

    AcompanamientoPersistenceAdapter(AcompanamientoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Acompanamiento> obtenerOCrear(List<String> nombres) {
        List<String> claves = nombres.stream().map(Acompanamiento::clave).toList();
        Map<String, AcompanamientoEntity> existentes = jpa.buscarPorClaves(claves).stream()
                .collect(Collectors.toMap(e -> Acompanamiento.clave(e.getNombre()), Function.identity()));
        return nombres.stream()
                .map(nombre -> existentes.computeIfAbsent(Acompanamiento.clave(nombre),
                        k -> jpa.save(new AcompanamientoEntity(nombre))))
                .map(AcompanamientoEntity::aDominio)
                .toList();
    }
}
