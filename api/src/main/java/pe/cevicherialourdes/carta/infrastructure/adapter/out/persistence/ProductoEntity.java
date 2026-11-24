package pe.cevicherialourdes.carta.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    protected ProductoEntity() {
    }

    static ProductoEntity desde(Producto p) {
        ProductoEntity e = new ProductoEntity();
        e.id = p.getId();
        e.nombre = p.getNombre();
        e.categoria = p.getCategoria();
        e.precio = p.getPrecio() == null ? null : p.getPrecio().monto();
        e.requiereCocina = p.isRequiereCocina();
        e.disponible = p.isDisponible();
        e.activo = p.isActivo();
        return e;
    }

    Producto aDominio() {
        return Producto.reconstituir(id, nombre, categoria, precio == null ? null : Dinero.de(precio),
                requiereCocina, disponible, activo);
    }
}
