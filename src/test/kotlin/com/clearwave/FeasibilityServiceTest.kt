package com.clearwave

import com.clearwave.domain.LineProfile
import com.clearwave.feasibility.FeasibilityRequest
import com.clearwave.feasibility.FeasibilityResponse
import com.clearwave.stubs.FeasibilityScenario
import com.clearwave.support.ClearwaveExtension.Companion.feasibilityService
import com.clearwave.support.ClearwaveExtension.Companion.fibreVisionStub
import com.clearwave.support.ClearwaveExtension.Companion.httpClient
import com.clearwave.support.ClearwaveExtension.Companion.openNetworkStub
import com.clearwave.support.ClearwaveTest
import com.clearwave.support.TelecomsCapturedOutputs.FeasibilityResult
import com.clearwave.support.TelecomsFixtures.serviceAddress
import com.clearwave.support.TelecomsFixtures.trackingId
import com.clearwave.support.TelecomsParty
import com.clearwave.support.TrackingId
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.kensa.Action
import dev.kensa.ActionContext
import dev.kensa.GivensContext
import dev.kensa.Notes
import dev.kensa.StateCollector
import dev.kensa.render.Language
import dev.kensa.state.CapturedInteractionBuilder.Companion.from
import dev.kensa.util.Attributes
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Notes("""
**Feasibility Service** queries both suppliers for every postcode regardless of the service package requested.
Profiles from both suppliers are merged and sorted by download speed (fastest first).

Supplier integrations:
- [OpenNetwork](#) — voice and broadband, **JSON** protocol
- [FibreVision](#) — broadband only, **XML** protocol
""")
class FeasibilityServiceTest : ClearwaveTest() {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `address is serviceable by both suppliers`() {
        given(bothSuppliersAreServiceable())

        whenever(aFeasibilityCheckIsRequestedForTheServiceAddress())

        then(theFeasibilityResult(), shouldBeServiceableWithThreeProfilesFastestFirst())
    }

    @Test
    fun `address is not serviceable by open network`() {
        given(openNetworkHasNoCoverage())
        and(fibreVisionIsServiceable())

        whenever(aFeasibilityCheckIsRequestedForTheServiceAddress())

        then(theFeasibilityResult(), shouldBeServiceableFromFibreVisionOnly())
    }

    @Test
    fun `address is not serviceable by either supplier`() {
        given(openNetworkHasNoCoverage())
        and(fibreVisionHasNoCoverage())

        whenever(aFeasibilityCheckIsRequestedForTheServiceAddress())

        then(theFeasibilityResult(), shouldNotBeServiceable())
    }

    @Test
    fun `open network supplier error is handled gracefully`() {
        given(openNetworkIsUnavailable())
        and(fibreVisionIsServiceable())

        whenever(aFeasibilityCheckIsRequestedForTheServiceAddress())

        then(theFeasibilityResult(), shouldBeServiceableFromFibreVisionOnly())
    }

    // --- Givens ---

    private fun bothSuppliersAreServiceable() = Action<GivensContext> { (fixtures) ->
        val tid = fixtures[trackingId]
        openNetworkStub.primeFeasibility(tid, FeasibilityScenario.Serviceable(openNetworkProfiles()))
        fibreVisionStub.primeFeasibility(tid, FeasibilityScenario.Serviceable(fibreVisionProfiles()))
    }

    private fun openNetworkHasNoCoverage() = Action<GivensContext> { (fixtures) ->
        openNetworkStub.primeFeasibility(fixtures[trackingId], FeasibilityScenario.NotServiceable)
    }

    private fun fibreVisionIsServiceable() = Action<GivensContext> { (fixtures) ->
        fibreVisionStub.primeFeasibility(fixtures[trackingId], FeasibilityScenario.Serviceable(fibreVisionProfiles()))
    }

    private fun fibreVisionHasNoCoverage() = Action<GivensContext> { (fixtures) ->
        fibreVisionStub.primeFeasibility(fixtures[trackingId], FeasibilityScenario.NotServiceable)
    }

    private fun openNetworkIsUnavailable() = Action<GivensContext> { (fixtures) ->
        openNetworkStub.primeFeasibility(fixtures[trackingId], FeasibilityScenario.SupplierError)
    }

    // --- Action ---

    private fun aFeasibilityCheckIsRequestedForTheServiceAddress() = Action<ActionContext> { (fixtures, interactions) ->
        val tid = fixtures[trackingId]
        openNetworkStub.register(tid, interactions)
        fibreVisionStub.register(tid, interactions)

        val requestBody = mapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(FeasibilityRequest(fixtures[serviceAddress]))

        val httpRequest = Request(POST, "http://localhost:${feasibilityService.port}/api/feasibility")
            .header(TrackingId.HEADER, tid.toString())
            .header("Content-Type", "application/json")
            .body(requestBody)

        interactions.capture(
            from(TelecomsParty.Customer)
                .to(TelecomsParty.FeasibilityService)
                .with(requestBody, "Feasibility Request")
                .with(Attributes.of("language", Language.Json))
                .underTest(true)
        )

        val response = httpClient(httpRequest)
        outputs[FeasibilityResult] = mapper.readValue<FeasibilityResponse>(response.bodyString())

        interactions.capture(
            from(TelecomsParty.FeasibilityService)
                .to(TelecomsParty.Customer)
                .with(response, "Feasibility Response")
        )
    }

    // --- State Collectors ---

    private fun theFeasibilityResult() = StateCollector { outputs[FeasibilityResult] }

    // --- Assertions ---

    private fun shouldBeServiceableWithThreeProfilesFastestFirst() = Matcher<FeasibilityResponse> { result ->
        MatcherResult(
            result.serviceable && result.profiles.size == 3 && result.profiles.first().downloadSpeed == 900,
            { "Expected serviceable with 3 profiles sorted fastest-first (900 Mbps), got: ${result.profiles}" },
            { "Expected not to be serviceable with three profiles" }
        )
    }

    private fun shouldBeServiceableFromFibreVisionOnly() = Matcher<FeasibilityResponse> { result ->
        MatcherResult(
            result.serviceable && result.profiles.size == 1 && result.profiles.single().supplier == "FibreVision",
            { "Expected serviceable with one FibreVision profile, got: ${result.profiles}" },
            { "Expected not to be solely serviceable from FibreVision" }
        )
    }

    private fun shouldNotBeServiceable() = Matcher<FeasibilityResponse> { result ->
        MatcherResult(
            !result.serviceable && result.profiles.isEmpty(),
            { "Expected not serviceable with no profiles, got: serviceable=${result.serviceable}, profiles=${result.profiles}" },
            { "Expected to be not serviceable" }
        )
    }

    // --- Test data ---

    private fun openNetworkProfiles() = listOf(
        LineProfile("FTTP", 900, 110, "Full Fibre 900", "OpenNetwork"),
        LineProfile("FTTP", 500, 75,  "Full Fibre 500", "OpenNetwork"),
    )

    private fun fibreVisionProfiles() = listOf(
        LineProfile("FTTC", 80, 20, "Superfast 80", "FibreVision"),
    )
}
