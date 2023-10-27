package terraform.flattener

import java.io.File
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AppTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("getInputs")
    fun `verifying inputs match outputs`(dirName: String) {
        File("build/test-outputs/").mkdir()
        main(arrayOf(File("src/test/resources/inputs/$dirName").canonicalPath, File("build/test-outputs/$dirName.tf").canonicalPath))
        assertEquals(File("src/test/resources/outputs/$dirName.tf").readText(), File("build/test-outputs/$dirName.tf").readText())
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun `pre cleanup`() {
            File("build/test-outputs").deleteRecursively()
        }
        @JvmStatic
        fun getInputs(): List<Arguments> = File("src/test/resources/inputs").listFiles().orEmpty().map {
            Arguments.of(it.name)
        }
    }
}
