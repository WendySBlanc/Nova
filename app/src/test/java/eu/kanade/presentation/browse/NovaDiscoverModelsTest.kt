package eu.kanade.presentation.browse

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaDiscoverModelsTest {

    @Test
    fun `source failures expose a useful recovery action`() {
        toNovaSourceError(IllegalStateException("HTTP 403 access denied")) shouldBe NovaSourceError(
            kind = NovaSourceErrorKind.AccessDenied,
            recovery = NovaSourceRecovery.OpenWebView,
        )
        toNovaSourceError(IllegalStateException("Attestation refused")) shouldBe NovaSourceError(
            kind = NovaSourceErrorKind.Attestation,
            recovery = NovaSourceRecovery.OpenWebView,
        )
        toNovaSourceError(IllegalStateException("unsupported reader signed build")) shouldBe NovaSourceError(
            kind = NovaSourceErrorKind.UnsupportedExtension,
            recovery = NovaSourceRecovery.UpdateExtension,
        )
    }

    @Test
    fun `malformed server responses never expose parser internals`() {
        toNovaSourceError(IllegalArgumentException("JsonDecodingException unexpected JSON token")) shouldBe
            NovaSourceError(
                kind = NovaSourceErrorKind.InvalidResponse,
                recovery = NovaSourceRecovery.Retry,
            )
    }

    @Test
    fun `extension bulk selection keeps only actionable entries`() {
        val entries = listOf(
            NovaExtensionEntry("one", NovaExtensionState.UpdateAvailable),
            NovaExtensionEntry("two", NovaExtensionState.Installed),
            NovaExtensionEntry("three", NovaExtensionState.Incompatible),
        )

        selectActionableExtensions(entries).map { it.packageName } shouldBe listOf("one", "two")
    }
}
