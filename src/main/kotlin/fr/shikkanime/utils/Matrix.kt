package fr.shikkanime.utils

data class Matrix(
    val rows: Int,
    val columns: Int,
    val data: FloatArray = FloatArray(rows * columns)
) {
    operator fun get(row: Int, column: Int) = data[row * columns + column]

    operator fun set(row: Int, column: Int, value: Float) {
        data[row * columns + column] = value
    }

    fun copy() = Matrix(rows, columns, data.copyOf())

    fun map(block: (Float, Int, Int) -> Float): Matrix {
        for (x in 0..<rows) {
            for (y in 0..<columns) {
                val value = get(x, y)
                set(x, y, block(value, x, y))
            }
        }

        return this
    }

    private fun getBroadcasted(row: Int, column: Int) = this[if (rows == 1) 0 else row, if (columns == 1) 0 else column]

    operator fun times(other: Matrix): Matrix {
        val resRows = maxOf(rows, other.rows)
        val resCols = maxOf(columns, other.columns)

        return Matrix(resRows, resCols).map { _, row, column ->
            this.getBroadcasted(row, column) * other.getBroadcasted(row, column)
        }
    }

    fun sumRows() = Matrix(1, columns).map { _, _, column ->
        var sum = 0f
        for (row in 0 until rows) {
            sum += this[row, column]
        }
        sum
    }

    fun sumColumns() = Matrix(rows, 1).map { _, row, _ ->
        var sum = 0f
        for (column in 0 until columns) {
            sum += this[row, column]
        }
        sum
    }

    fun normalize(): Matrix {
        val copy = copy()
        val sum = copy.data.sum()
        return copy.map { value, _, _ -> value / sum }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Matrix) return false

        if (rows != other.rows) return false
        if (columns != other.columns) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + columns
        result = 31 * result + data.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "Matrix(rows=$rows, columns=$columns, data=${data.contentToString()})"
    }
}
