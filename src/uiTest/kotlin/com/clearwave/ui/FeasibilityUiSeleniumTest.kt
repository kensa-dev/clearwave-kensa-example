package com.clearwave.ui

import com.clearwave.domain.LineProfile
import com.clearwave.stubs.FeasibilityScenario
import com.clearwave.ui.ClearwaveUiExtension.Companion.web
import dev.kensa.Action
import dev.kensa.ActionContext
import dev.kensa.GivensContext
import dev.kensa.StateCollector
import dev.kensa.kotest.WithKotest
import dev.kensa.selenium.SeleniumBrowserDriver
import dev.kensa.selenium.junit.KensaSeleniumUiTest
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import org.junit.jupiter.api.Test

class FeasibilityUiSeleniumTest : ClearwaveUiTest, KensaSeleniumUiTest<FeasibilityUserSelenium>(), WithKotest {

    override fun createUser(driver: SeleniumBrowserDriver): FeasibilityUserSelenium =
        FeasibilityUserSelenium(driver, web.webUrl)

    @Test
    fun `address with fibre availability shows the fibre profile`() {
        given(theFeasibilityServiceCanOfferFibre())
        and(theUserOpensTheFeasibilityPage())
        and(theUserEntersAServiceableAddress())
        whenever(theUserSubmitsTheForm())
        then(theNumberOfAvailableProfiles(), shouldShowAtLeastOneProfile())
    }

    @Test
    fun `address with no service shows a not-serviceable message`() {
        given(theFeasibilityServiceReportsNoService())
        and(theUserOpensTheFeasibilityPage())
        and(theUserEntersAnUnservicedAddress())
        whenever(theUserSubmitsTheForm())
        then(theNotServiceableMessage(), shouldBeDisplayed())
    }

    @Test
    fun `empty postcode shows a validation error`() {
        given(theFeasibilityServiceReportsNoService())
        and(theUserOpensTheFeasibilityPage())
        and(theUserEntersAnAddressWithNoPostcode())
        whenever(theUserSubmitsTheForm())
        then(thePostcodeValidationError(), shouldBeDisplayed())
    }

    // --- Givens ---

    private fun theFeasibilityServiceCanOfferFibre() = Action<GivensContext> { _ ->
        web.primeFor(FeasibilityScenario.Serviceable(profiles = listOf(
            LineProfile("FTTP", 1000, 200, "Full Fibre", "OpenNetwork"),
        )))
    }

    private fun theFeasibilityServiceReportsNoService() = Action<GivensContext> { _ ->
        web.primeFor(FeasibilityScenario.NotServiceable)
    }

    private fun theUserOpensTheFeasibilityPage() = Action<GivensContext> { _ ->
        theUser.navigatesToTheFeasibilityPage()
    }

    private fun theUserEntersAServiceableAddress() = Action<GivensContext> { _ ->
        theUser.entersTheAddress("SE1 7TY", "1 Borough High St", "London", "Greater London")
    }

    private fun theUserEntersAnUnservicedAddress() = Action<GivensContext> { _ ->
        theUser.entersTheAddress("ZZ99 9ZZ", "Unknown St", "Nowhere", "Outback")
    }

    private fun theUserEntersAnAddressWithNoPostcode() = Action<GivensContext> { _ ->
        theUser.entersTheAddress("", "1 Borough High St", "London", "Greater London")
    }

    // --- Action ---

    private fun theUserSubmitsTheForm() = Action<ActionContext> { _ ->
        theUser.submitsTheForm()
    }

    // --- State Collectors ---

    private fun theNumberOfAvailableProfiles() = StateCollector { theUser.seesAvailableProfiles() }
    private fun theNotServiceableMessage() = StateCollector { theUser.seesNoServiceMessage() }
    private fun thePostcodeValidationError() = StateCollector { theUser.seesAValidationErrorOnPostcode() }

    // --- Matchers ---

    private fun shouldShowAtLeastOneProfile() = Matcher<Int> { count ->
        MatcherResult(
            count > 0,
            { "Expected at least one profile card on the page, but the count was $count" },
            { "Expected no profile cards on the page" }
        )
    }

    private fun shouldBeDisplayed() = Matcher<Boolean> { displayed ->
        MatcherResult(
            displayed,
            { "Expected element to be displayed but it was not" },
            { "Expected element not to be displayed but it was" }
        )
    }
}
