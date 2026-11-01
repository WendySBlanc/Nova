package mihon.domain.extensionrepo.model

data class ExtensionRepo(
    val baseUrl: String,
    val name: String,
    val shortName: String?,
    val website: String,
    val signingKeyFingerprint: String,
)

const val NOVA_EXTENSION_REPO_BASE_URL = "https://raw.githubusercontent.com/WendySBlanc/extensions/repo"
const val NOVA_EXTENSION_REPO_INDEX_URL = "$NOVA_EXTENSION_REPO_BASE_URL/index.pb"
const val NOVA_SIGNING_KEY_FINGERPRINT =
    "f25a2ca5f93f7af24de9afff1a5afc2cf7600d3d87af03d8497a0ae4865de701"
const val LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT =
    "9add655a78e96c4ec7a53ef89dccb557cb5d767489fac5e785d671a5a75d4da2"
