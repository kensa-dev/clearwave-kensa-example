package com.clearwave.ui

import dev.kensa.Kensa.konfigure
import dev.kensa.junit.KensaExtension
import dev.kensa.uitesting.uiTesting
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.io.path.Path

@ExtendWith(KensaExtension::class)
class ClearwaveUiExtension : BeforeAllCallback, AutoCloseable {

    init {
        konfigure {
            titleText                       = "Clearwave Telecoms — UI Tests"
            sourceLocations                 = listOf(Path("src/uiTest/kotlin"))
            uiTesting.autoScreenshotOnFailure = true
        }
    }

    override fun beforeAll(context: ExtensionContext) {
        val rootStore = context.root.getStore(ExtensionContext.Namespace.GLOBAL)
        rootStore.computeIfAbsent("CLEARWAVE_UI_WEB") {
            web = WebFixture()
            this
        }
    }

    override fun close() {
        web.close()
    }

    companion object {
        lateinit var web: WebFixture
            private set
    }
}
