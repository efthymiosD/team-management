package com.decoupledx.teammanagement.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import spock.lang.Ignore
import spock.lang.Specification

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * ADR-0001 rules 4/7/9/10 and ADR-0003, enforced project-wide (ported from the
 * Reservation System's ModuleStructureTest + RecordPlacementTest).
 */
class ModuleStructureSpec extends Specification {

    private static final JavaClasses CLASSES =
            new ClassFileImporter().importPackages("com.decoupledx.teammanagement")

    // rule 4: inbound ports are public interfaces in <module>.domain.port
    private static final DescribedPredicate<JavaClass> PORT_CLASSES = DescribedPredicate
            .describe("inbound port classes of any module", candidate ->
                    candidate.getPackageName().contains(".domain.port"))

    // rules 7 + 9: controllers drive their own domain ports
    private static final DescribedPredicate<JavaClass> CONTROLLERS = DescribedPredicate
            .describe("web controllers of any module", candidate ->
                    candidate.getPackageName().contains(".adapter")
                            && !candidate.getPackageName().contains(".adapter.api")
                            && candidate.getSimpleName().endsWith("Controller"))

    private static final DescribedPredicate<JavaClass> HAND_WRITTEN_NESTED =
            new DescribedPredicate<JavaClass>("hand-written nested types") {
                @Override
                boolean test(JavaClass input) {
                    def name = input.getName()
                    if (!name.contains('$')) {
                        return false
                    }
                    // Compiler-generated artifacts are exempt: Lombok builders,
                    // anonymous Outer$1..$n, and Groovy's closure classes.
                    def segments = name.split('\\$')
                    boolean groovyClosure = segments.any { it.toLowerCase().contains('closure') }
                    return !groovyClosure
                            && !(name ==~ /.*\$\w*Builder$/)
                            && !(name ==~ /.*\$\d+$/)
                }
            }

    def "inbound ports are public interfaces"() {
        expect:
        classes()
                .that(PORT_CLASSES)
                .should().bePublic()
                .andShould().beInterfaces()
                .because("ADR-0001 rule 4: inbound ports are public interfaces "
                        + "accessible from the module's adapter package")
                .check(CLASSES)
    }

    def "web controllers never depend on the persistence adapter (ADR-0001 rule 7)"() {
        expect:
        noClasses()
                .that(CONTROLLERS)
                .should().dependOnClassesThat()
                .resideInAnyPackage("..adapter.persistence..")
                .because("ADR-0001: controllers drive inbound ports; the"
                        + " persistence adapter is a sibling they may never touch."
                        + " (Controller-local request records in the same"
                        + " .adapter.web package are deliberately NOT flagged.)")
                .check(CLASSES)
    }

    def "every type is a top-level type (records in their own files)"() {
        expect:
        noClasses()
                .that(HAND_WRITTEN_NESTED)
                .should().beNestedClasses()
                .because("nested records hurt discoverability (ADR-0003): "
                        + "every record gets its own top-level file")
                .allowEmptyShould(true)
                .check(CLASSES)
    }
}
