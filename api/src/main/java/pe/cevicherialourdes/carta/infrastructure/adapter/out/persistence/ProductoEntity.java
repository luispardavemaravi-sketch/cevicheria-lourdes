package pe.cevicherialourdes.carta.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import pe.cevicherialourdes.carta.domain.model.Categoria;
import pe.cevicherialourdes.carta.domain.model.Producto;
import pe.cevicherialourdes.shared.domain.model.Dinero;

@Entity
@Table(name = "producto")
class ProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60, unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Column(precision = 8, scale = 2)
    private BigDecimal precio;

    @Column(name = "requiere_cocina", nullable = false)
    private boolean requiereCocina;

    @Column(nullable = false)
    private boolean disponible;

    @Column(nullable = false)
    private boolean activo;

    /**
     * Sin orphanRemoval: las presentaciones nunca se borran, se desactivan (HU-04).
     * Se cargan en lote para toda la pagina (hibernate.default_batch_fetch_size), no una consulta por producto.
     */
    @OneToMany(mappedBy = "producto", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<PresentacionEntity> presentaciones = new ArrayList<>();

    /** HU-05. Sin cascada: el catalogo de acompanamientos se administra aparte. */
    @ManyToMany
    @JoinTable(name = "producto_acompanamiento",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "acompanamiento_id"))
    private Set<AcompanamientoEntity> acompanamientos = new HashSet<>();

    protected ProductoEntity() {
    }

    /** @param referencia convierte un id de acompanamiento en una referencia gestionada por JPA */
    static ProductoEntity desde(Producto p, Function<Long, AcompanamientoEntity> referencia) {
        ProductoEntity e = new ProductoEntity();
        e.id = p.getId();
        e.nombre = p.getNombre();
        e.categoria = p.getCategoria();
        e.precio = p.getPrecio() == null ? null : p.getPrecio().monto();
        e.requiereCocina = p.isRequiereCocina();
        e.disponible = p.isDisponible();
        e.activo = p.isActivo();
        p.getPresentaciones().forEach(pr -> e.presentaciones.add(PresentacionEntity.desde(pr, e)));
        p.getAcompanamientos().forEach(a -> e.acompanamientos.add(referencia.apply(a.id())));
        return e;
    }

    Producto aDominio() {
        return Producto.reconstituir(id, nombre, categoria, precio == null ? null : Dinero.de(precio),
                requiereCocina, disponible, activo,
                presentaciones.stream().map(PresentacionEntity::aDominio).toList(),
                acompanamientos.stream().map(AcompanamientoEntity::aDominio).toList());
    }
}
