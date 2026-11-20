package eu.kanade.tachiyomi.data.lens

import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ProofbirdFrenchCorrectorTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `corrector batches the page and stops when every bubble is stable`() = runTest {
        val labels = listOf("\$KEEP", "\$REPLACE_est", "\$REPLACE_sont", "\$TRANSFORM_SUF__e", "\$TRANSFORM_AGREEMENT_PLURAL")
        val runtime = FakeProofbirdGectorRuntime(
            predictions = ArrayDeque(
                listOf(
                    listOf(
                        intArrayOf(0, 0, 0, 1, 0, 3),
                        intArrayOf(0, 0, 2, 0, 4),
                    ),
                    listOf(
                        intArrayOf(0, 0, 0, 0, 0, 0),
                        intArrayOf(0, 0, 0, 0, 0),
                    ),
                ),
            ),
        )
        val tokenizer = ProofbirdWordPieceTokenizer(
            listOf(
                "[PAD]", "[UNK]", "[CLS]", "[SEP]", "Ma", "soeur", "sont", "très", "content", "contente",
                "Ils", "est", "déjà", "parti", "sont", "partis",
            ),
        )
        val corrector = ProofbirdFrenchCorrector(
            modelProvider = { modelFile() },
            runtime = runtime,
            tokenizer = tokenizer,
            labels = labels,
        )

        corrector.correctPage(
            linkedMapOf(
                "b0" to "Ma soeur sont très content.",
                "b1" to "Ils est déjà parti.",
            ),
        ) shouldContainExactly linkedMapOf(
            "b0" to "Ma soeur est très contente.",
            "b1" to "Ils sont déjà partis.",
        )

        runtime.calls shouldBe 2
        runtime.batchSizes shouldBe listOf(2, 2)
    }

    @Test
    fun `corrector bounds dense pages to eight bubbles per inference`() = runTest {
        val translations = (0 until 10).associateTo(linkedMapOf()) { index -> "b$index" to "Bonjour" }
        val runtime = FakeProofbirdGectorRuntime(
            ArrayDeque(
                listOf(
                    List(8) { intArrayOf(0, 0) },
                    List(2) { intArrayOf(0, 0) },
                ),
            ),
        )
        val corrector = ProofbirdFrenchCorrector(
            modelProvider = { modelFile() },
            runtime = runtime,
            tokenizer = ProofbirdWordPieceTokenizer(listOf("[PAD]", "[UNK]", "[CLS]", "[SEP]", "Bonjour")),
            labels = listOf("\$KEEP"),
        )

        corrector.correctPage(translations) shouldContainExactly translations

        runtime.batchSizes shouldBe listOf(8, 2)
    }

    @Test
    fun `corrector preserves the original surface when no edit is needed`() = runTest {
        val corrector = ProofbirdFrenchCorrector(
            modelProvider = { modelFile() },
            runtime = FakeProofbirdGectorRuntime(ArrayDeque(listOf(listOf(intArrayOf(0, 0, 0, 0, 0))))),
            tokenizer = ProofbirdWordPieceTokenizer(
                listOf("[PAD]", "[UNK]", "[CLS]", "[SEP]", "L", "'", "homme", "est", "déjà", "là"),
            ),
            labels = listOf("\$KEEP"),
        )

        corrector.correctPage(linkedMapOf("b0" to "L’homme est déjà là.")) shouldContainExactly linkedMapOf(
            "b0" to "L’homme est déjà là.",
        )
    }

    @Test
    fun `corrector preserves line breaks apostrophes and French punctuation around edits`() = runTest {
        val corrector = ProofbirdFrenchCorrector(
            modelProvider = { modelFile() },
            runtime = FakeProofbirdGectorRuntime(
                ArrayDeque(
                    listOf(
                        listOf(intArrayOf(0, 0, 0, 1, 0, 2)),
                        listOf(intArrayOf(0, 0, 0, 0, 0, 0)),
                    ),
                ),
            ),
            tokenizer = ProofbirdWordPieceTokenizer(
                listOf("[PAD]", "[UNK]", "[CLS]", "[SEP]", "Ma", "sœur", "sont", "est", "très", "content", "contente", "!"),
            ),
            labels = listOf("\$KEEP", "\$REPLACE_est", "\$TRANSFORM_SUF__e"),
        )

        corrector.correctPage(linkedMapOf("b0" to "Ma sœur sont\ntrès content !")) shouldContainExactly linkedMapOf(
            "b0" to "Ma sœur est\ntrès contente !",
        )
    }

    @Test
    fun `corrector applies the French ou rule after model inference`() = runTest {
        val corrector = ProofbirdFrenchCorrector(
            modelProvider = { modelFile() },
            runtime = FakeProofbirdGectorRuntime(
                ArrayDeque(listOf(listOf(IntArray(8) { 0 }))),
            ),
            tokenizer = ProofbirdWordPieceTokenizer(
                listOf("[PAD]", "[UNK]", "[CLS]", "[SEP]", "Je", "ne", "sais", "pas", "ou", "il", "habite"),
            ),
            labels = listOf("\$KEEP"),
        )

        corrector.correctPage(linkedMapOf("b0" to "Je ne sais pas ou il habite.")) shouldContainExactly linkedMapOf(
            "b0" to "Je ne sais pas où il habite.",
        )
    }

    private fun modelFile(): File = temporaryDirectory.resolve("proofbird.onnx").toFile().apply {
        if (!exists()) writeText("model")
    }
}

private class FakeProofbirdGectorRuntime(
    private val predictions: ArrayDeque<List<IntArray>>,
) : ProofbirdGectorRuntime {
    var calls = 0
    val batchSizes = mutableListOf<Int>()

    override suspend fun predict(modelFile: File, encodings: List<ProofbirdEncoding>): List<IntArray> {
        calls++
        batchSizes += encodings.size
        return predictions.removeFirst()
    }

    override fun close() = Unit
}
