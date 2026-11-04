package com.decoupledx.teammanagement.architecture

import com.decoupledx.common.event.DomainEventListener
import com.decoupledx.common.event.OnDomainEvent
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import org.springframework.modulith.events.ApplicationModuleListener
import spock.lang.Specification

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * Domain purity (ported from the Reservation System's DomainPurityTest, ADR-0001
 * rule 3 + ADR-0004): domain packages stay framework-free; event reactions live
 * only in adapter/listeners and implement the shared contract.
 */
class DomainPuritySpec extends Specification {

    private static final JavaClasses CLASSES =
            new ClassFileImporter().importPackages("com.decoupledx.teammanagement")

    // package-info classes carry only structural metadata (e.g. Modulith named
    // interfaces) and contain no domain logic; module configs are the single
    // sanctioned framework touchpoint.
    private static final DescribedPredicate<JavaClass> DOMAIN_CLASSES = DescribedPredicate
            .describe("domain classes other than package-info and configs", candidate ->
                    candidate.getPackageName().contains(".domain")
                            && candidate.getSimpleName() != "package-info"
                            && !candidate.getSimpleName().contains("Config"))

    private static final DescribedPredicate<JavaClass> CARRIES_REACTION_METHOD = DescribedPredicate
            .describe("carries an @OnDomainEvent method", type -> type.getMethods().stream()
                    .anyMatch(method -> method.isAnnotatedWith(OnDomainEvent.class)))

    def "domain packages do not depend on Spring or persistence frameworks"() {
        expect:
        noClasses()
                .that(DOMAIN_CLASSES)
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..")
                .check(CLASSES)
    }

    def "domain packages do not depend on web or security frameworks"() {
        expect:
        noClasses()
                .that(DOMAIN_CLASSES)
                .should().dependOnClassesThat()
                .resideInAnyPackage("jakarta.servlet..", "org.springframework.web..", "org.springframework.security..")
                .check(CLASSES)
    }

    def "event reaction handlers live only in adapter/listeners packages"() {
        expect:
        CLASSES.stream()
                .filter(CARRIES_REACTION_METHOD)
                .filter(type -> !type.getPackageName().matches(".+[.]adapter[.]listeners"))
                .toList()
                .isEmpty()
    }

    def "the platform listener annotation appears only inside adapter/listeners"() {
        expect:
        CLASSES.stream()
                .filter(type -> !type.getPackageName().matches(".+[.]adapter[.]listeners"))
                .filter(type -> type.getAllMethods().stream()
                        .anyMatch(method -> method.isAnnotatedWith(ApplicationModuleListener.class)))
                .toList()
                .isEmpty()
    }

    def "listeners implement the shared DomainEventListener contract"() {
        expect:
        classes()
                .that(CARRIES_REACTION_METHOD)
                .should().implement(DomainEventListener.class)
                .because("listener classes conform to the per-module event contract (ADR-0004)")
                .check(CLASSES)
    }
}
