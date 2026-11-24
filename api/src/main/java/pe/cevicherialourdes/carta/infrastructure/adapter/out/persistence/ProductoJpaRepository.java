package pe.cevicherialourdes.carta.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.cevicherialourdes.carta.domain.model.Categoria;

interface ProductoJpaRepository extends JpaRepository<ProductoEntity, Long> {

    /** lower(nombre) usa el indice unico ux_producto_nombre_ci (V3). */
    @Query("""
            select count(p) > 0 from ProductoEntity p
            where lower(p.nombre) = lower(:nombre) and (:excluirId is null or p.id <> :excluirId)
            """)
    boolean existeNombre(@Param("nombre") String nombre, @Param("excluirId") Long excluirId);

    @Query("""
            select p from ProductoEntity p
            where (:categoria is null or p.categoria = :categoria)
              and (:incluirInactivos = true or p.activo = true)
            """)
    Page<ProductoEntity> buscar(@Param("categoria") Categoria categoria,
                                @Param("incluirInactivos") boolean incluirInactivos,
                                Pageable pageable);
}
