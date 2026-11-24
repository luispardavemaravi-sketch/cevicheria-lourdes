package pe.cevicherialourdes.carta.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AcompanamientoJpaRepository extends JpaRepository<AcompanamientoEntity, Long> {

    /** lower(nombre) usa el indice unico ux_acompanamiento_nombre_ci (V5). Acotado por el tamano de la lista. */
    @Query("select a from AcompanamientoEntity a where lower(a.nombre) in :claves")
    List<AcompanamientoEntity> buscarPorClaves(@Param("claves") Collection<String> claves);
}
