package nova.cardmotion

import androidx.compose.ui.unit.dp
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CardMotionSpecTest {

    @Test
    fun `motion values must be valid`() {
        shouldThrow<IllegalArgumentException> {
            CardMotionSpec(durationMillis = -1)
        }
        shouldThrow<IllegalArgumentException> {
            CardMotionSpec(blurRadius = (-1).dp)
        }
    }

    @Test
    fun `reduced motion removes spatial effects`() {
        val resolved = CardMotionSpec(
            durationMillis = 260,
            blurRadius = 18.dp,
        ).resolved(CardMotionMode.Reduced)

        resolved.durationMillis shouldBe 0
        resolved.blurRadius shouldBe 0.dp
    }

    @Test
    fun `spatial motion keeps configured effects`() {
        val resolved = CardMotionSpec(
            durationMillis = 260,
            blurRadius = 18.dp,
        ).resolved(CardMotionMode.Spatial)

        resolved.durationMillis shouldBe 260
        resolved.blurRadius shouldBe 18.dp
    }
}
