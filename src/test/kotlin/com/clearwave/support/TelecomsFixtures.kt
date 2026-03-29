package com.clearwave.support

import com.clearwave.domain.AppointmentSlot
import com.clearwave.domain.LineProfile
import com.clearwave.domain.ServiceAddress
import dev.kensa.fixture.FixtureContainer
import dev.kensa.fixture.fixture
import java.time.LocalDate

object TelecomsFixtures : FixtureContainer {

    /** Unique correlation token for each test invocation. */
    val trackingId = fixture("Tracking Id", highlighted = true) { TrackingId() }

    /** The customer's postcode — used as the key for feasibility enquiries. */
    val postcode = fixture("Postcode") { "SW1A 2AA" }

    /** Full service installation address derived from the postcode. */
    val serviceAddress = fixture("Service Address", postcode) { pc ->
        ServiceAddress(
            postcode     = pc,
            addressLine1 = "1 Parliament Square",
            town         = "Westminster",
            county       = "London",
        )
    }

    /** Voice (PSTN) profile available via OpenNetwork. */
    val voiceProfile = fixture("Voice Profile") {
        LineProfile(
            type          = "FTTP",
            downloadSpeed = 900,
            uploadSpeed   = 110,
            description   = "Full Fibre 900 with Voice",
            supplier      = "OpenNetwork",
        )
    }

    /** Broadband profile available via FibreVision. */
    val broadbandProfile = fixture("Broadband Profile") {
        LineProfile(
            type          = "FTTC",
            downloadSpeed = 80,
            uploadSpeed   = 20,
            description   = "Superfast 80",
            supplier      = "FibreVision",
        )
    }

    /** Engineer visit slot for FTTP installations requiring physical work. */
    val appointmentSlot = fixture("Appointment Slot") {
        AppointmentSlot(
            date     = LocalDate.now().plusDays(7),
            timeSlot = "AM",
        )
    }
}
