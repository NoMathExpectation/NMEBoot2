package NoMathExpectation.NMEBoot.util

import java.sql.ResultSet

fun ResultSet.toReadableText(): String {
    val metadata = metaData
    val columnCount = metadata.columnCount

    val stringTable = buildList {
        add(
            (1..columnCount).map { metadata.getColumnName(it) }
        )

        while (next()) {
            add(
                (1..columnCount).map { getString(it) ?: "NULL" }
            )
        }
    }

    val columnWidths = List(columnCount) { colIndex ->
        stringTable.maxOf { row -> row[colIndex].length }
    }

    return buildString {
        for (rowIndex in stringTable.indices) {
            val row = stringTable[rowIndex]
            for (colIndex in row.indices) {
                append(row[colIndex].padEnd(columnWidths[colIndex] + 2))
            }
            appendLine()
            if (rowIndex == 0) {
                append("-".repeat(columnWidths.sum() + columnCount * 2 - 2))
                append("  ")
                appendLine()
            }
        }
    }
}