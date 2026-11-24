package pe.cevicherialourdes.carta.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.cevicherialourdes.carta.domain.model.Acompanamiento;

@Entity
@Table(name = "acompanamiento")
class AcompanamientoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, unique = true)
    private String nombre;

    protected AcompanamientoEntity() {
    }

    AcompanamientoEntity(String nombre) {
        this.nombre = nombre;
    }

    String getNombre() {
        return nombre;
    }

    Acompanamiento aDominio() {
        return new Acompanamiento(id, nombre);
    }
}
