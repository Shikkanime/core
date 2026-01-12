package fr.shikkanime.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MatrixTest {
    @Test
    fun testMatrixOperations() {
        val userMatrix = Matrix(3, 1)
        userMatrix[0, 0] = 7f
        userMatrix[1, 0] = 4f
        userMatrix[2, 0] = 10f

        val matrix = Matrix(3, 5)
        matrix[0, 2] = 1f
        matrix[0, 4] = 1f

        matrix[1, 3] = 1f

        matrix[2, 0] = 1f
        matrix[2, 4] = 1f

        val result = userMatrix * matrix
        println(result)

        assertEquals(3, result.rows)
        assertEquals(5, result.columns)
        assertEquals(7f, result[0, 2])
        assertEquals(7f, result[0, 4])
        assertEquals(4f, result[1, 3])
        assertEquals(10f, result[2, 0])
        assertEquals(10f, result[2, 4])
        assertEquals(0f, result[0, 0])

        val result2 = result.sumRows()
        println(result2)

        assertEquals(10f, result2[0, 0])
        assertEquals(0f, result2[0, 1])
        assertEquals(7f, result2[0, 2])
        assertEquals(4f, result2[0, 3])
        assertEquals(17f, result2[0, 4])

        val normalized = result2.normalize()
        println(normalized)

        assertEquals(0.2631579f, normalized[0, 0])
        assertEquals(0f, normalized[0, 1])
        assertEquals(0.18421052f, normalized[0, 2])
        assertEquals(0.10526316f, normalized[0, 3])
        assertEquals(0.4473684f, normalized[0, 4])

        val matrix2 = Matrix(4, 5)
        matrix2[0, 0] = 1f
        matrix2[0, 1] = 1f
        matrix2[0, 4] = 1f

        matrix2[1, 0] = 1f
        matrix2[1, 1] = 1f
        matrix2[1, 3] = 1f

        matrix2[2, 1] = 1f
        matrix2[2, 2] = 1f
        matrix2[2, 4] = 1f

        matrix2[3, 3] = 1f

        val result3 = matrix2 * normalized
        println(result3)

        val sumColumns = result3.sumColumns()
        println(sumColumns)
    }
}