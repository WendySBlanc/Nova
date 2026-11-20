package eu.kanade.tachiyomi.data.lens

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ProofbirdGectorDecoderTest {

    @Test
    fun `cased WordPiece encoding keeps the first token of every source word`() {
        val tokenizer = ProofbirdWordPieceTokenizer(
            listOf("[PAD]", "[UNK]", "[CLS]", "[SEP]", "Bon", "##jour", "monde", "!"),
        )

        val encoding = tokenizer.encode(listOf("Bonjour", "monde", "!"))

        encoding.inputIds.toList() shouldContainExactly listOf(2L, 4L, 5L, 6L, 7L, 3L)
        encoding.firstTokenIndices.toList() shouldContainExactly listOf(0, 1, 3, 4)
    }

    @Test
    fun `unknown source words use the unknown token without losing word alignment`() {
        val tokenizer = ProofbirdWordPieceTokenizer(
            listOf("[PAD]", "[UNK]", "[CLS]", "[SEP]", "connu"),
        )

        val encoding = tokenizer.encode(listOf("inconnu", "connu"))

        encoding.inputIds.toList() shouldContainExactly listOf(2L, 1L, 4L, 3L)
        encoding.firstTokenIndices.toList() shouldContainExactly listOf(0, 1, 2)
    }

    @Test
    fun `GECToR edit tags fix agreement and preserve punctuation`() {
        val words = proofbirdWords("Ma soeur sont très content")
        val labels = listOf(
            "\$KEEP",
            "\$KEEP",
            "\$KEEP",
            "\$REPLACE_est",
            "\$KEEP",
            "\$TRANSFORM_SUF__e",
        )

        val corrected = applyProofbirdEdits(words, labels)

        joinProofbirdWords(corrected) shouldBe "Ma soeur est très contente"
    }

    @Test
    fun `GECToR append delete and merge tags reconstruct natural French spacing`() {
        val words = listOf("Bonjour", ",", "cher", "ami", "!")
        val labels = listOf(
            "\$KEEP",
            "\$APPEND_à",
            "\$DELETE",
            "\$KEEP",
            "\$KEEP",
            "\$KEEP",
        )

        val corrected = applyProofbirdEdits(words, labels)

        joinProofbirdWords(corrected) shouldBe "Bonjour à cher ami!"
    }

    @Test
    fun `word extraction keeps a French apostrophe inside one word`() {
        proofbirdWords("L'homme n'est pas là.") shouldContainExactly listOf("L'homme", "n'est", "pas", "là", ".")
    }

    @Test
    fun `runtime selects word labels from compact argmax output`() {
        val encodings = listOf(
            ProofbirdEncoding(longArrayOf(2, 10, 11, 3), intArrayOf(0, 1, 2)),
            ProofbirdEncoding(longArrayOf(2, 20, 3), intArrayOf(0, 1)),
        )

        selectProofbirdWordLabels(
            labelIds = arrayOf(longArrayOf(7, 8, 9, 0), longArrayOf(1, 2, 0, 0)),
            encodings = encodings,
        ).map(IntArray::toList) shouldContainExactly listOf(
            listOf(7, 8, 9),
            listOf(1, 2),
        )
    }
}
