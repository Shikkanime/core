package fr.shikkanime.wrappers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AniDBWrapperTest {
    @Test
    fun searchAniDBAnimeTitle() {
        val list = listOf<Pair<String, Set<Long>>>(
            "GG1U2N1E1" to setOf(18218),
            "G2XU048EG" to setOf(17784),
            "GMKUX4JGZ" to setOf(15690),
            "GK9U35GKD" to setOf(15936),
            "GPWUKPN4V" to setOf(15299),
            "G2XUNX5DZ" to setOf(18786),
            "G2XU0W1JW" to setOf(17427),
            "GN7UD435E" to setOf(15876),
            "G2XU04WV3" to setOf(17781),
            "G0DUNKM8K" to setOf(17320),
            "GJWUQ7KV0" to setOf(18336),
            "GZ7UD378J" to setOf(17615),
            "GG1U23JJZ" to setOf(16855),
            "G0DUN5X9V" to setOf(17005),
            "G31UX449N" to setOf(17221),
            "G14U415N4" to setOf(17999),
            "GG1U2430Q" to setOf(17950),
            "G0DUMG8KJ" to setOf(18290),
            "GN7UD2NQ9" to setOf(16250),
            "G31UXD2Q3" to setOf(17208),
            "G8WUN832Z" to setOf(14416),
            "GMKUX19XZ" to setOf(18153),
            "GMKUX83KJ" to setOf(16078),
            "GWDU8JJN8" to setOf(18040),
            "GQJUG3WZ3" to setOf(18101),
            "GD9UE92W1" to setOf(18258),
            "GQJUG102D" to setOf(13945),
            "G50UZEZ31" to setOf(15989),
            "G4VUQ92DW" to setOf(15918),
            "G9DUEM93N" to setOf(17771),
            "GK9U383E3" to setOf(14678),
            "G9DUE88QW" to setOf(16793),
            "G9DUE88QW" to setOf(16793),
            "GEVUZ87N4" to setOf(16261),
            "GEVUZED4Z" to setOf(15954),
            "GK9U30ZPP" to setOf(17484),
            "GYDKX92G6" to setOf(14156),
            "GMKUX1KV2" to setOf(17635),
            "G6JQZ0EWR" to setOf(12385),
            "GN7UDVX45" to setOf(17159),
            "G64P55DPR" to setOf(11828),
            "GYNQ21EWY" to setOf(11689),
            "GR9VVV5W6" to setOf(11523),
            "G63K954Q6" to setOf(14230),
            "GN7UDWW2P" to setOf(15601),
            "G6VN2MEDR" to setOf(10944),
            "G14U4MNQ8" to setOf(11992),
            "GEVUZGQMV" to setOf(14511),
            "GD9UV8QPM" to setOf(17198),
            "G8WUN0P2D" to setOf(17198),
            "G50UMKW3Q" to setOf(18249),
            "GWDU8QDXD" to setOf(69),
            "G7PU41833" to setOf(18229),
            "GK9U35334" to setOf(16564),
            "GWDU8XQVW" to setOf(16700),
            "G14UVQ5D5" to setOf(69),
            "GD9UE98D4" to setOf(69),

            "G50UMK4P0" to setOf(8286), // Natsume's Book of Friends Season 3
            "GRVDWWG3R" to setOf(12942), // The Ancient Magus' Bride S1
            "GWDU8JDMX" to setOf(12665),

            // Multiple values
            // Not correct, "ReZero Season 2 Part 2"
//            "G14U4357Z" to setOf(14792, 17947),
        )

        list.forEach { (episodeId, aniDbAnimeIds) ->
            val exactMatches = AniDBWrapper.searchAniDBAnimeTitle(setOf("en-US", "fr-FR"), episodeId)
            println("Exact matches for $episodeId: $exactMatches")
            assertEquals(aniDbAnimeIds, exactMatches.keys.map { it.key }.toSet())
        }
    }
}