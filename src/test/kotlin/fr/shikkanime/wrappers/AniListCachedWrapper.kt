package fr.shikkanime.wrappers

import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.wrappers.impl.caches.AniListCachedWrapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZonedDateTime
import java.util.stream.Stream

class AniListCachedWrapper  : AbstractTest() {
    data class MediaTestCase(
        val name: String,
        val platforms: List<AnimePlatform>? = null,
        val firstReleasedYear: Int? = null,
        val latestReleaseDateTime: ZonedDateTime,
        val exceptedMediaId: Int,
    )

    companion object {
        @JvmStatic
        fun mediaTestCases(): Stream<MediaTestCase> = Stream.of(
            MediaTestCase(
                name = "Mobile Suit Gundam the Witch from Mercury",
                latestReleaseDateTime = ZonedDateTime.parse("2023-07-02T09:30:00Z"),
                exceptedMediaId = 139274
            ),
            MediaTestCase(
                name = "The Yakuza's Guide to Babysitting",
                latestReleaseDateTime = ZonedDateTime.parse("2022-09-22T15:30:00Z"),
                exceptedMediaId = 138882
            ),
            MediaTestCase(
                name = "Endô and Kobayashi Live!",
                latestReleaseDateTime = ZonedDateTime.parse("2023-03-24T17:55:00Z"),
                exceptedMediaId = 143064
            ),
            MediaTestCase(
                name = "NIGHT HEAD 2041",
                latestReleaseDateTime = ZonedDateTime.parse("2021-09-29T18:00:00Z"),
                exceptedMediaId = 125868
            ),
            MediaTestCase(
                name = "Code Geass",
                latestReleaseDateTime = ZonedDateTime.parse("2008-09-28T21:00:00Z"),
                exceptedMediaId = 1575
            ),
            MediaTestCase(
                name = "Dragon Ball DAIMA",
                latestReleaseDateTime = ZonedDateTime.parse("2025-03-07T15:10:00Z"),
                exceptedMediaId = 170083
            ),
            MediaTestCase(
                name = "High Card",
                latestReleaseDateTime = ZonedDateTime.parse("2024-03-25T12:00:00Z"),
                exceptedMediaId = 135778
            ),
            MediaTestCase(
                name = "Love Live! Nijigasaki High School Idol Club",
                latestReleaseDateTime = ZonedDateTime.parse("2023-10-23T15:30:00Z"),
                exceptedMediaId = 113970
            ),
            MediaTestCase(
                name = "Muv-Luv Alternative",
                latestReleaseDateTime = ZonedDateTime.parse("2022-12-21T18:25:00Z"),
                exceptedMediaId = 112716
            ),
            MediaTestCase(
                name = "FAIRY TAIL: 100 YEARS QUEST",
                latestReleaseDateTime = ZonedDateTime.parse("2025-03-01T00:00:00Z"),
                exceptedMediaId = 139095
            ),
            MediaTestCase(
                name = "Kaina of the Great Snow Sea",
                latestReleaseDateTime = ZonedDateTime.parse("2023-12-22T01:00:00Z"),
                exceptedMediaId = 144144
            ),
            MediaTestCase(
                name = "I Was Reincarnated as the 7th Prince so I Can Take My Time Perfecting My Magical Ability",
                latestReleaseDateTime = ZonedDateTime.parse("2025-09-24T16:30:00Z"),
                exceptedMediaId = 156415
            ),
            MediaTestCase(
                name = "Seirei Gensouki: Spirit Chronicles",
                latestReleaseDateTime = ZonedDateTime.parse("2024-12-23T18:30:00Z"),
                exceptedMediaId = 126546
            ),
            MediaTestCase(
                name = "Gokinjo, une vie de quartier",
                latestReleaseDateTime = ZonedDateTime.parse("1996-09-01T08:00:00Z"),
                exceptedMediaId = 852
            ),
            MediaTestCase(
                name = "Dragon Ball GT",
                latestReleaseDateTime = ZonedDateTime.parse("2023-01-04T16:00:00Z"),
                exceptedMediaId = 225
            ),
            MediaTestCase(
                name = "Spy Classroom",
                latestReleaseDateTime = ZonedDateTime.parse("2023-09-28T14:30:00Z"),
                exceptedMediaId = 146323
            ),
            MediaTestCase(
                name = "In Another World With My Smartphone",
                latestReleaseDateTime = ZonedDateTime.parse("2023-06-19T16:00:00Z"),
                exceptedMediaId = 98491
            ),
            MediaTestCase(
                name = "Trigun",
                latestReleaseDateTime = ZonedDateTime.parse("1998-10-01T16:00:00Z"),
                exceptedMediaId = 6
            ),
            MediaTestCase(
                name = "Rent-a-Girlfriend",
                latestReleaseDateTime = ZonedDateTime.parse("2025-09-16T13:30:00Z"),
                exceptedMediaId = 113813
            ),
            MediaTestCase(
                name = "Kenshin le vagabond (2023)",
                latestReleaseDateTime = ZonedDateTime.parse("2025-03-20T18:00:00Z"),
                exceptedMediaId = 142877
            ),
            MediaTestCase(
                name = "Lovely Complex",
                latestReleaseDateTime = ZonedDateTime.parse("2007-09-29T08:00:00Z"),
                exceptedMediaId = 2034
            ),
            MediaTestCase(
                name = "Ranma1/2",
                firstReleasedYear = 2024,
                latestReleaseDateTime = ZonedDateTime.parse("2025-10-04T17:00:00Z"),
                exceptedMediaId = 178533
            ),
            MediaTestCase(
                name = "Mission: Yozakura Family",
                latestReleaseDateTime = ZonedDateTime.parse("2024-10-27T07:00:00Z"),
                exceptedMediaId = 158898
            ),
            MediaTestCase(
                name = "Air Gear",
                latestReleaseDateTime = ZonedDateTime.parse("2007-03-21T15:00:00Z"),
                exceptedMediaId = 857
            ),
            MediaTestCase(
                name = "School Rumble",
                latestReleaseDateTime = ZonedDateTime.parse("2006-09-25T15:00:00Z"),
                exceptedMediaId = 24
            ),
            MediaTestCase(
                name = "The Rising of the Shield Hero",
                latestReleaseDateTime = ZonedDateTime.parse("2025-09-24T12:30:00Z"),
                exceptedMediaId = 99263
            ),
            MediaTestCase(
                name = "Arknights",
                firstReleasedYear = 2022,
                latestReleaseDateTime = ZonedDateTime.parse("2025-09-05T15:00:00Z"),
                exceptedMediaId = 140660
            ),
            MediaTestCase(
                name = "Kimagure Orange Road",
                firstReleasedYear = 1987,
                latestReleaseDateTime = ZonedDateTime.parse("1991-04-01T08:00:00Z"),
                exceptedMediaId = 1087
            ),
            MediaTestCase(
                name = "One Room",
                firstReleasedYear = 2017,
                latestReleaseDateTime = ZonedDateTime.parse("2020-12-21T18:15:00Z"),
                exceptedMediaId = 97857
            ),
            MediaTestCase(
                name = "Terminator Zero",
                firstReleasedYear = 2024,
                latestReleaseDateTime = ZonedDateTime.parse("2024-08-29T07:01:00Z"),
                exceptedMediaId = 177814
            ),
            MediaTestCase(
                name = "Mon chat à tout faire est encore tout déprimé",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G79H23Z3P")),
                firstReleasedYear = 2023,
                latestReleaseDateTime = ZonedDateTime.parse("2023-09-29T18:45:00Z"),
                exceptedMediaId = 149883
            ),
            MediaTestCase(
                name = "Akame ga Kill !",
                firstReleasedYear = 2014,
                latestReleaseDateTime = ZonedDateTime.parse("2014-12-15T15:00:00Z"),
                exceptedMediaId = 20613
            ),
            MediaTestCase(
                name = "My Friend's Little Sister Has It In for Me!",
                firstReleasedYear = 2025,
                latestReleaseDateTime = ZonedDateTime.parse("2025-10-04T17:30:00Z"),
                exceptedMediaId = 129195
            ),
            MediaTestCase(
                name = "SHY",
                firstReleasedYear = 2023,
                latestReleaseDateTime = ZonedDateTime.parse("2024-09-23T16:00:00Z"),
                exceptedMediaId = 155389
            ),
            MediaTestCase(
                name = "L'épée de Kamui",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1175")),
                firstReleasedYear = 1985,
                latestReleaseDateTime = ZonedDateTime.parse("1985-03-09T07:00:00Z"),
                exceptedMediaId = 496
            ),
            MediaTestCase(
                name = "Kamisama : Opération Divine",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GNVHKNP4J")),
                firstReleasedYear = 2023,
                latestReleaseDateTime = ZonedDateTime.parse("2023-07-05T16:30:00Z"),
                exceptedMediaId = 148048
            ),
            MediaTestCase(
                name = "Kamisama Kiss",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1215")),
                firstReleasedYear = 2012,
                latestReleaseDateTime = ZonedDateTime.parse("2015-03-31T07:00:00Z"),
                exceptedMediaId = 14713
            ),
            MediaTestCase(
                name = "SAINT SEIYA: Knights of the Zodiac",
                firstReleasedYear = 1987,
                latestReleaseDateTime = ZonedDateTime.parse("2024-06-10T08:00:00Z"),
                exceptedMediaId = 1254
            ),
            MediaTestCase(
                name = "Signé Cat's Eyes",
                platforms = listOf(AnimePlatform(platform = Platform.DISN, platformId = "21240dd9-5fb4-4334-be13-687a6bd230f7")),
                firstReleasedYear = 2025,
                latestReleaseDateTime = ZonedDateTime.parse("2025-10-03T07:00:00Z"),
                exceptedMediaId = 184718
            ),
            MediaTestCase(
                name = "Le château de Cagliostro",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G79H23W89")),
                firstReleasedYear = 1979,
                latestReleaseDateTime = ZonedDateTime.parse("1979-12-15T17:00:00Z"),
                exceptedMediaId = 1430
            ),
            MediaTestCase(
                name = "Hyakka Ryouran",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1113")),
                firstReleasedYear = 2010,
                latestReleaseDateTime = ZonedDateTime.parse("2013-06-21T08:00:00Z"),
                exceptedMediaId = 8277
            ),
            MediaTestCase(
                name = "Umamusume: Pretty Derby",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GR79P2816")),
                firstReleasedYear = 2021,
                latestReleaseDateTime = ZonedDateTime.parse("2023-12-27T17:00:00Z"),
                exceptedMediaId = 98514
            ),
            MediaTestCase(
                name = "Mobile Suit Gundam: Cucuruz Doan's Island",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GEXH3W2J5")),
                firstReleasedYear = 2022,
                latestReleaseDateTime = ZonedDateTime.parse("2022-06-03T01:00:00Z"),
                exceptedMediaId = 139273
            ),
            MediaTestCase(
                name = "Perfect Blue",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GZJH3D8V3")),
                firstReleasedYear = 1998,
                latestReleaseDateTime = ZonedDateTime.parse("2023-06-13T17:00:00Z"),
                exceptedMediaId = 437
            ),
            MediaTestCase(
                name = "PSYCHO-PASS",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GR75253JY")),
                firstReleasedYear = 2019,
                latestReleaseDateTime = ZonedDateTime.parse("2020-03-27T22:30:00Z"),
                exceptedMediaId = 13601
            ),
            MediaTestCase(
                name = "The New Chronicles of Extraordinary Beings: Preface",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1323")),
                firstReleasedYear = 2025,
                latestReleaseDateTime = ZonedDateTime.parse("2025-08-29T09:00:00Z"),
                exceptedMediaId = 156104
            ),
            MediaTestCase(
                name = "Kakuriyo -Bed & Breakfast for Spirits-",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G6MEK1ZGR")),
                firstReleasedYear = 2018,
                latestReleaseDateTime = ZonedDateTime.parse("2025-10-01T15:30:00Z"),
                exceptedMediaId = 100500
            ),
            MediaTestCase(
                name = "Mon histoire d'amour avec Yamada à Lv999",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GNVHKNPQ7")),
                firstReleasedYear = 2023,
                latestReleaseDateTime = ZonedDateTime.parse("2023-06-24T17:00:00Z"),
                exceptedMediaId = 154965
            ),
            MediaTestCase(
                name = "ONE PIECE",
                firstReleasedYear = 1999,
                latestReleaseDateTime = ZonedDateTime.parse("2025-10-04T10:00:00Z"),
                exceptedMediaId = 21
            ),
            MediaTestCase(
                name = "Go! Princess Pretty Cure",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1213")),
                firstReleasedYear = 2015,
                latestReleaseDateTime = ZonedDateTime.parse("2016-01-31T07:00:00Z"),
                exceptedMediaId = 21001
            ),
            MediaTestCase(
                name = "Yuru Camp – Au grand air",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GRWEW95KR")),
                firstReleasedYear = 2018,
                latestReleaseDateTime = ZonedDateTime.parse("2024-10-23T15:00:00Z"),
                exceptedMediaId = 98444
            ),
            MediaTestCase(
                name = "Le Conte des parias",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GVDHX8QZX")),
                firstReleasedYear = 2023,
                latestReleaseDateTime = ZonedDateTime.parse("2023-04-02T13:30:00Z"),
                exceptedMediaId = 151679
            ),
            MediaTestCase(
                name = "Nana",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GVDHX8QGP")),
                firstReleasedYear = 2006,
                latestReleaseDateTime = ZonedDateTime.parse("2007-03-29T19:30:00Z"),
                exceptedMediaId = 877
            ),
            MediaTestCase(
                name = "Chouchouté par l’ange d’à côté",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G9VHN91DJ")),
                firstReleasedYear = 2023,
                latestReleaseDateTime = ZonedDateTime.parse("2023-03-25T15:00:00Z"),
                exceptedMediaId = 143338
            ),
            MediaTestCase(
                name = "Super Cub",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GJ0H7QM1J")),
                firstReleasedYear = 2021,
                latestReleaseDateTime = ZonedDateTime.parse("2021-06-23T15:00:00Z"),
                exceptedMediaId = 113418
            ),
            MediaTestCase(
                name = "NINTAMA RANTARŌ: MAÎTRE INVINCIBLE DES NINJAS DOKUTAKE",
                platforms = listOf(AnimePlatform(platform = Platform.PRIM, platformId = "0ITMHDR4DYNJDXLMAJ627H0ELR")),
                firstReleasedYear = 2024,
                latestReleaseDateTime = ZonedDateTime.parse("2024-12-20T22:00:00Z"),
                exceptedMediaId = 175138
            ),
            MediaTestCase(
                name = "Mawaru Penguindrum",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "212")),
                firstReleasedYear = 2011,
                latestReleaseDateTime = ZonedDateTime.parse("2011-12-23T16:00:00Z"),
                exceptedMediaId = 10721
            ),
            MediaTestCase(
                name = "Au Cœur du Donjon",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1209")),
                firstReleasedYear = 2024,
                latestReleaseDateTime = ZonedDateTime.parse("2024-09-27T17:30:00Z"),
                exceptedMediaId = 168345
            ),
            MediaTestCase(
                name = "Comment Raeliana a survécu au manoir Wynknight",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GVDHX8Q71")),
                firstReleasedYear = 2023,
                latestReleaseDateTime = ZonedDateTime.parse("2023-06-26T13:00:00Z"),
                exceptedMediaId = 151847
            ),
            MediaTestCase(
                name = "Dragon Ball",
                firstReleasedYear = 1986,
                latestReleaseDateTime = ZonedDateTime.parse("2022-12-15T16:00:00Z"),
                exceptedMediaId = 223
            ),
            MediaTestCase(
                name = "Digimon Adventure: (2020)",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "606"), AnimePlatform(platform = Platform.CRUN, platformId = "GYEX24PV6"), AnimePlatform(platform = Platform.ANIM, platformId = "1153")),
                firstReleasedYear = 2020,
                latestReleaseDateTime = ZonedDateTime.parse("2024-03-07T08:00:00Z"),
                exceptedMediaId = 114811
            ),
            MediaTestCase(
                name = "Le puissant dragon végan",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G1XHJV2NJ")),
                firstReleasedYear = 2022,
                latestReleaseDateTime = ZonedDateTime.parse("2024-12-18T04:00:00Z"),
                exceptedMediaId = 141879
            ),
            MediaTestCase(
                name = "Dororo",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1075")),
                firstReleasedYear = 2019,
                latestReleaseDateTime = ZonedDateTime.parse("2019-06-24T15:00:00Z"),
                exceptedMediaId = 101347
            ),
            MediaTestCase(
                name = "La réceptionniste Pokémon",
                platforms = listOf(AnimePlatform(platform = Platform.NETF, platformId = "81186864")),
                firstReleasedYear = 2023,
                latestReleaseDateTime = ZonedDateTime.parse("2025-09-04T05:00:00Z"),
                exceptedMediaId = 162147
            ),
            MediaTestCase(
                name = "Sorairo Utility",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1268")),
                firstReleasedYear = 2025,
                latestReleaseDateTime = ZonedDateTime.parse("2025-03-21T17:00:00Z"),
                exceptedMediaId = 174596
            ),
            MediaTestCase(
                name = "KONOSUBA -God's blessing on this wonderful world!",
                firstReleasedYear = 2016,
                latestReleaseDateTime = ZonedDateTime.parse("2025-04-25T01:00:00Z"),
                exceptedMediaId = 21202
            ),
            MediaTestCase(
                name = "À quoi tu joues, Ayumu ?!",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "913")),
                firstReleasedYear = 2022,
                latestReleaseDateTime = ZonedDateTime.parse("2022-09-23T17:35:00Z"),
                exceptedMediaId = 128223
            ),
            MediaTestCase(
                name = "Tokyo Ghoul",
                firstReleasedYear = 2014,
                latestReleaseDateTime = ZonedDateTime.parse("2020-11-24T17:00:00Z"),
                exceptedMediaId = 20605
            ),
            MediaTestCase(
                name = "Vavam Vampire",
                platforms = listOf(AnimePlatform(platform = Platform.NETF, platformId = "81949674")),
                firstReleasedYear = 2025,
                latestReleaseDateTime = ZonedDateTime.parse("2025-03-29T15:00:00Z"),
                exceptedMediaId = 175422
            ),
            MediaTestCase(
                name = "Arion",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1174")),
                firstReleasedYear = 1986,
                latestReleaseDateTime = ZonedDateTime.parse("1986-03-15T07:00:00Z"),
                exceptedMediaId = 791
            ),
            MediaTestCase(
                name = "Saiyuki Gaiden",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "830")),
                firstReleasedYear = 2011,
                latestReleaseDateTime = ZonedDateTime.parse("2013-06-26T17:00:00Z"),
                exceptedMediaId = 9088
            ),
            MediaTestCase(
                name = "My Isekai Life",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "908")),
                firstReleasedYear = 2022,
                latestReleaseDateTime = ZonedDateTime.parse("2022-09-12T12:00:00Z"),
                exceptedMediaId = 129192
            )
        )
    }

    @ParameterizedTest
    @MethodSource("mediaTestCases")
    fun fetchMedia(testCase: MediaTestCase) {
        val media = runBlocking { AniListCachedWrapper.findAnilistMedia(testCase.name, testCase.platforms, testCase.firstReleasedYear, testCase.latestReleaseDateTime) }
        assertNotNull(media)
        assertEquals(testCase.exceptedMediaId, media!!.id)
    }
}
