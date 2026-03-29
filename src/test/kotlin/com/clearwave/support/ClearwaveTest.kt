package com.clearwave.support

import dev.kensa.UseSetupStrategy
import dev.kensa.junit.KensaTest
import dev.kensa.kotest.WithKotest
import dev.kensa.state.SetupStrategy
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ClearwaveExtension::class)
@UseSetupStrategy(SetupStrategy.Grouped)
abstract class ClearwaveTest : KensaTest, WithKotest
