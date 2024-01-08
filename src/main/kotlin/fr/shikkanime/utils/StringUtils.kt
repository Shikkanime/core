package fr.shikkanime.utils

object StringUtils {
    fun getShortName(fullName: String): String {
        val regexs = listOf("-.*-".toRegex(), "Saison \\d*".toRegex())
        val separators = listOf(":", ",")
        var shortName = fullName

        separators.forEach { separator ->
            if (shortName.contains(separator)) {
                val split = shortName.split(separator)
                val firstPart = split[0].trim()
                val lastPart = split.subList(1, split.size).joinToString(" ").trim()

                if (lastPart.count { it == ' ' } >= 2) {
                    shortName = firstPart
                }
            }
        }

        regexs.forEach { regex ->
            shortName = regex.replace(shortName, "")
        }

        shortName = shortName.replace(" +".toRegex(), " ")
        return shortName.trim()
    }
}