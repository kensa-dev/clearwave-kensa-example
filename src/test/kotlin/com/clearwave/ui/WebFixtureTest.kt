package com.clearwave.ui

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class WebFixtureTest {

    @Test
    fun `serves the built UI at root`() {
        WebFixture().use { fixture ->
            val response = JavaHttpClient()(Request(GET, "${fixture.webUrl}/"))
            response.status shouldBe OK
            response.bodyString() shouldContain "<div id=\"root\">"
        }
    }
}
