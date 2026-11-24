package pe.cevicherialourdes.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Reglas de la arquitectura hexagonal. Si alguien las rompe, el build falla en CI. */
@AnalyzeClasses(packages = "pe.cevicherialourdes", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule dominio_sin_frameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "org.hibernate..", "com.fasterxml..")
            .because("el dominio es Java puro: reglas de negocio sin framework");

    @ArchTest
    static final ArchRule dominio_no_conoce_capas_externas = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
            .because("las dependencias apuntan hacia el dominio, nunca al reves");

    @ArchTest
    static final ArchRule aplicacion_no_conoce_infraestructura = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .allowEmptyShould(true)
            .because("los casos de uso hablan con puertos, no con adaptadores");

    @ArchTest
    static final ArchRule controllers_sin_repositorios = noClasses()
            .that().resideInAPackage("..adapter.in.web..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..", "org.springframework.data..")
            .allowEmptyShould(true)
            .because("el controller solo llama al puerto de entrada");
}
