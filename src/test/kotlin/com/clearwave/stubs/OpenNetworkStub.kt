package com.clearwave.stubs

import com.clearwave.feasibility.OpenNetworkFeasibilityResponse
import com.clearwave.feasibility.OpenNetworkProfile
import com.clearwave.order.OpenNetworkNotification
import com.clearwave.order.OpenNetworkOrderResponse
import com.clearwave.support.TelecomsParty
import com.clearwave.support.TrackingId
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.kensa.render.Language
import dev.kensa.state.CapturedInteractionBuilder.Companion.from
import dev.kensa.state.CapturedInteractions
import dev.kensa.util.Attributes
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Test stub for the OpenNetwork supplier (JSON API).
 *
 * **Priming** — call [primeFeasibility] / [primeOrder] during `given` with the [TrackingId]
 * fixture value to configure what the stub returns for that test invocation.
 *
 * **Interaction capture** — call [register] during `whenever` to attach the test's
 * [CapturedInteractions] so that the stub records sequence diagram arrows automatically.
 *
 * **State inspection** — after the test action, call [feasibilityRequestFor],
 * [feasibilityResponseFor], [orderRequestFor] etc. from a [StateCollector] to assert
 * on the exact messages sent to and received from the supplier.
 */
class OpenNetworkStub(val port: Int = findAvailablePort()) : AutoCloseable {

    private val mapper = jacksonObjectMapper()
    private val registry = TrackingRegistry()
    private val httpClient = JavaHttpClient()
    private val orderCounter = AtomicInteger(0)

    private val feasibilityScenarios = ConcurrentHashMap<String, FeasibilityScenario>()
    private val feasibilityRequests  = ConcurrentHashMap<String, String>()
    private val feasibilityResponses = ConcurrentHashMap<String, String>()

    private val orderScenarios  = ConcurrentHashMap<String, OrderScenario>()
    private val orderRequests   = ConcurrentHashMap<String, String>()
    private val orderResponses  = ConcurrentHashMap<String, String>()

    private val server = routes(
        "/api/feasibility/check" bind POST to ::handleFeasibility,
        "/api/orders"            bind POST to ::handleOrder,
    ).asServer(SunHttp(port))

    fun start()          = apply { server.start() }
    override fun close() { server.stop() }

    // --- Registration ---

    fun register(trackingId: TrackingId, interactions: CapturedInteractions) =
        registry.register(trackingId, interactions)

    fun unregister(trackingId: TrackingId) = registry.unregister(trackingId)

    // --- Priming ---

    fun primeFeasibility(trackingId: TrackingId, scenario: FeasibilityScenario) {
        feasibilityScenarios[trackingId.toString()] = scenario
    }

    fun primeOrder(trackingId: TrackingId, scenario: OrderScenario) {
        orderScenarios[trackingId.toString()] = scenario
    }

    // --- State inspection (for StateCollectors) ---

    fun feasibilityRequestFor(trackingId: TrackingId): String? = feasibilityRequests[trackingId.toString()]
    fun feasibilityResponseFor(trackingId: TrackingId): String? = feasibilityResponses[trackingId.toString()]
    fun orderRequestFor(trackingId: TrackingId): String? = orderRequests[trackingId.toString()]
    fun orderResponseFor(trackingId: TrackingId): String? = orderResponses[trackingId.toString()]

    // --- Handlers ---

    private fun handleFeasibility(request: Request): Response {
        val tid = request.header(TrackingId.HEADER) ?: return Response(INTERNAL_SERVER_ERROR)
        val interactions = registry.forRequest(request)
        val requestBody = request.bodyString()

        feasibilityRequests[tid] = requestBody
        interactions.capture(
            from(TelecomsParty.FeasibilityService)
                .to(TelecomsParty.OpenNetwork)
                .with(requestBody, "Feasibility Check Request")
                .with(Attributes.of("language", Language.Json))
        )

        val scenario = feasibilityScenarios[tid] ?: return Response(INTERNAL_SERVER_ERROR)

        val (response, responseBody) = buildFeasibilityResponse(scenario)
        feasibilityResponses[tid] = responseBody
        interactions.capture(
            from(TelecomsParty.OpenNetwork)
                .to(TelecomsParty.FeasibilityService)
                .with(responseBody, "Feasibility Check Response")
                .with(Attributes.of("language", Language.Json))
        )

        return response
    }

    private fun buildFeasibilityResponse(scenario: FeasibilityScenario): Pair<Response, String> =
        when (scenario) {
            is FeasibilityScenario.Serviceable -> {
                val body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    OpenNetworkFeasibilityResponse(
                        available = true,
                        profiles  = scenario.profiles.map { p ->
                            OpenNetworkProfile(p.type, p.downloadSpeed, p.uploadSpeed, p.description)
                        }
                    )
                )
                Response(OK).header("Content-Type", "application/json").body(body) to body
            }
            FeasibilityScenario.NotServiceable -> {
                val body = mapper.writeValueAsString(
                    OpenNetworkFeasibilityResponse(available = false, reason = "No coverage at this postcode")
                )
                Response(OK).header("Content-Type", "application/json").body(body) to body
            }
            FeasibilityScenario.SupplierError -> {
                val body = """{"error": "OpenNetwork system unavailable"}"""
                Response(INTERNAL_SERVER_ERROR).header("Content-Type", "application/json").body(body) to body
            }
        }

    private fun handleOrder(request: Request): Response {
        val tid = request.header(TrackingId.HEADER) ?: return Response(INTERNAL_SERVER_ERROR)
        val interactions = registry.forRequest(request)
        val requestBody = request.bodyString()

        orderRequests[tid] = requestBody
        interactions.capture(
            from(TelecomsParty.OrderService)
                .to(TelecomsParty.OpenNetwork)
                .with(requestBody, "Place Order Request")
                .with(Attributes.of("language", Language.Json))
        )

        val scenario = orderScenarios[tid] ?: return Response(INTERNAL_SERVER_ERROR)
        val orderRef = "ON-${orderCounter.incrementAndGet().toString().padStart(5, '0')}"

        val responseBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
            OpenNetworkOrderResponse(orderRef = orderRef, status = "PENDING")
        )
        orderResponses[tid] = responseBody
        interactions.capture(
            from(TelecomsParty.OpenNetwork)
                .to(TelecomsParty.OrderService)
                .with(responseBody, "Order Accepted — Pending")
                .with(Attributes.of("language", Language.Json))
        )

        // Extract callback URL and fire async notifications on a background thread
        val callbackUrl = try {
            mapper.readValue<Map<String, Any>>(requestBody)["notificationCallbackUrl"] as? String
        } catch (_: Exception) { null }

        if (callbackUrl != null) {
            fireNotificationsAsync(orderRef, tid, callbackUrl, scenario, interactions)
        }

        return Response(OK).header("Content-Type", "application/json").body(responseBody)
    }

    private fun fireNotificationsAsync(
        orderRef: String,
        tid: String,
        callbackUrl: String,
        scenario: OrderScenario,
        interactions: CapturedInteractions,
    ) {
        val notifications = when (scenario) {
            is OrderScenario.Accepted -> scenario.notifications.map { it.name } to scenario.notificationDelayMs
            OrderScenario.Rejected    -> listOf("REJECTED") to 50L
        }

        thread(isDaemon = true, name = "on-notify-$orderRef") {
            val (statuses, delayMs) = notifications
            for (status in statuses) {
                Thread.sleep(delayMs)
                val body = mapper.writeValueAsString(
                    OpenNetworkNotification(orderRef = orderRef, status = status)
                )
                interactions.capture(
                    from(TelecomsParty.OpenNetwork)
                        .to(TelecomsParty.OrderService)
                        .with(body, "Order Notification — $status")
                        .with(Attributes.of("language", Language.Json))
                )
                try {
                    httpClient(
                        Request(POST, callbackUrl)
                            .header(TrackingId.HEADER, tid)
                            .header("Content-Type", "application/json")
                            .body(body)
                    )
                } catch (_: Exception) { /* test torn down */ }
            }
        }
    }

    companion object {
        fun findAvailablePort(): Int = ServerSocket(0).use { it.localPort }
    }
}
