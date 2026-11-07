package com.decoupledx.teammanagement.architecture

import com.decoupledx.teammanagement.TeamManagementApplication
import org.springframework.modulith.core.ApplicationModules
import spock.lang.Specification

/**
 * Spring Modulith boundary verification (carried over from the Reservation
 * System's ModularityTests): named-interface exports, internal references and
 * cross-module event cycles must all hold.
 */
class ModularitySpec extends Specification {

    def "modular structure verifies"() {
        expect:
        ApplicationModules.of(TeamManagementApplication).verify()
    }

    def "bootstrap exposes exactly the intended modules"() {
        when:
        def modules = ApplicationModules.of(TeamManagementApplication)
                .stream().toList()
                .collect { it.getDisplayName() }
                .sort()
        then:
        modules == ["Identity", "Shared", "Team"]
    }
}
