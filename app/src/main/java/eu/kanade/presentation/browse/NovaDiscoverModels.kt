package eu.kanade.presentation.browse

enum class NovaSourceErrorKind {
    Network,
    AccessDenied,
    Attestation,
    InvalidResponse,
    UnsupportedExtension,
    NotFound,
    Unknown,
}

enum class NovaSourceRecovery {
    Retry,
    OpenWebView,
    UpdateExtension,
}

data class NovaSourceError(
    val kind: NovaSourceErrorKind,
    val recovery: NovaSourceRecovery,
)

fun toNovaSourceError(error: Throwable): NovaSourceError {
    val details = buildString {
        append(error::class.simpleName.orEmpty())
        append(' ')
        append(error.message.orEmpty())
    }.lowercase()

    return when {
        "unsupported" in details && ("signed" in details || "reader" in details) -> NovaSourceError(
            NovaSourceErrorKind.UnsupportedExtension,
            NovaSourceRecovery.UpdateExtension,
        )
        "json" in details || "serialization" in details || "decoding" in details -> NovaSourceError(
            NovaSourceErrorKind.InvalidResponse,
            NovaSourceRecovery.Retry,
        )
        "attestation" in details -> NovaSourceError(
            NovaSourceErrorKind.Attestation,
            NovaSourceRecovery.OpenWebView,
        )
        "403" in details || "access denied" in details || "access refused" in details -> NovaSourceError(
            NovaSourceErrorKind.AccessDenied,
            NovaSourceRecovery.OpenWebView,
        )
        "404" in details || "not found" in details -> NovaSourceError(
            NovaSourceErrorKind.NotFound,
            NovaSourceRecovery.Retry,
        )
        "timeout" in details || "network" in details || "connect" in details -> NovaSourceError(
            NovaSourceErrorKind.Network,
            NovaSourceRecovery.Retry,
        )
        else -> NovaSourceError(NovaSourceErrorKind.Unknown, NovaSourceRecovery.Retry)
    }
}

enum class NovaExtensionState {
    Installed,
    UpdateAvailable,
    Disabled,
    Incompatible,
}

data class NovaExtensionEntry(
    val packageName: String,
    val state: NovaExtensionState,
)

fun selectActionableExtensions(entries: List<NovaExtensionEntry>): List<NovaExtensionEntry> =
    entries.filter { it.state != NovaExtensionState.Incompatible }
