package com.clearwave.ui

import com.clearwave.feasibility.FeasibilityService
import com.clearwave.stubs.FeasibilityScenario
import com.clearwave.stubs.FibreVisionStub
import com.clearwave.stubs.OpenNetworkStub
import com.clearwave.support.TrackingId
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference

class WebFixture(
    private val uiDistDir: File = File("ui/dist")
) : AutoCloseable {

    val openNetworkStub: OpenNetworkStub = OpenNetworkStub().start()
    val fibreVisionStub: FibreVisionStub = FibreVisionStub().start()

    private val feasibilityService: FeasibilityService =
        FeasibilityService(
            openNetworkUrl = "http://localhost:${openNetworkStub.port}",
            fibreVisionUrl = "http://localhost:${fibreVisionStub.port}",
        ).start()

    val webPort: Int = ServerSocket(0).use { it.localPort }
    val webUrl: String = "http://localhost:$webPort"

    private val httpClient: HttpHandler = JavaHttpClient()

    private val activeTrackingId = AtomicReference<TrackingId?>(null)

    fun primeFor(scenario: FeasibilityScenario): TrackingId {
        val trackingId = TrackingId()
        activeTrackingId.set(trackingId)
        openNetworkStub.primeFeasibility(trackingId, scenario)
        fibreVisionStub.primeFeasibility(trackingId, scenario)
        return trackingId
    }

    private val webHandler = routes(
        "/api/feasibility" bind POST to { request ->
            val trackingId = activeTrackingId.get() ?: error("WebFixture not primed — call primeFor() first")
            httpClient(
                request
                    .uri(request.uri.host("localhost").port(feasibilityService.port).scheme("http"))
                    .header("X-Tracking-Id", trackingId.toString())
            )
        },
        singlePageApp(ResourceLoader.Directory(uiDistDir.absolutePath))
    )

    private val webServer = webHandler.asServer(SunHttp(webPort))

    init {
        require(uiDistDir.exists()) {
            "UI build output not found at ${uiDistDir.absolutePath} — run `npm run build` from `ui/` first"
        }
        webServer.start()
    }

    override fun close() {
        webServer.stop()
        feasibilityService.close()
        fibreVisionStub.close()
        openNetworkStub.close()
    }
}
