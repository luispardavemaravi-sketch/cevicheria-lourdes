package pe.cevicherialourdes.carta.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pe.cevicherialourdes.carta.domain.model.Presentacion;
import pe.cevicherialourdes.shared.domain.model.Dinero;

@Entity
@Table(name = "presentacion")
class PresentacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoEntity producto;

    @Column(nullable = false, length = 30)
    private String nombre;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean activo;

    protected PresentacionEntity() {
    }

    static PresentacionEntity desde(Presentacion p, ProductoEntity producto) {
        PresentacionEntity e = new PresentacionEntity();
        e.id = p.getId();
        e.producto = producto;
        e.nombre = p.getNombre();
        e.precio = p.getPrecio().monto();
        e.activo = p.isActivo();
        return e;
    }

    Presentacion aDominio() {
        return Presentacion.reconstituir(id, nombre, Dinero.de(precio), activo);
    }
}
