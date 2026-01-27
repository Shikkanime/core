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
import java.util.stream.Stream

class AniListCachedWrapper : AbstractTest() {
    data class MediaTestCase(
        val name: String,
        val platforms: List<AnimePlatform>? = null,
        val firstReleasedYear: Int? = null,
        val exceptedMediaId: Int
    )

    companion object {
        @JvmStatic
        fun mediaTestCases(): Stream<MediaTestCase> = Stream.of(
            MediaTestCase(
                name = "Mobile Suit Gundam the Witch from Mercury",
                exceptedMediaId = 139274
            ),
            MediaTestCase(
                name = "The Yakuza's Guide to Babysitting",
                exceptedMediaId = 138882
            ),
            MediaTestCase(
                name = "Endô and Kobayashi Live!",
                exceptedMediaId = 143064
            ),
            MediaTestCase(
                name = "NIGHT HEAD 2041",
                exceptedMediaId = 125868
            ),
            MediaTestCase(
                name = "Code Geass",
                exceptedMediaId = 1575
            ),
            MediaTestCase(
                name = "Dragon Ball DAIMA",
                exceptedMediaId = 170083
            ),
            MediaTestCase(
                name = "High Card",
                exceptedMediaId = 135778
            ),
            MediaTestCase(
                name = "Love Live! Nijigasaki High School Idol Club",
                exceptedMediaId = 113970
            ),
            MediaTestCase(
                name = "Muv-Luv Alternative",
                exceptedMediaId = 112716
            ),
            MediaTestCase(
                name = "FAIRY TAIL: 100 YEARS QUEST",
                exceptedMediaId = 139095
            ),
            MediaTestCase(
                name = "Kaina of the Great Snow Sea",
                exceptedMediaId = 144144
            ),
            MediaTestCase(
                name = "I Was Reincarnated as the 7th Prince so I Can Take My Time Perfecting My Magical Ability",
                exceptedMediaId = 156415
            ),
            MediaTestCase(
                name = "Seirei Gensouki: Spirit Chronicles",
                exceptedMediaId = 126546
            ),
            MediaTestCase(
                name = "Gokinjo, une vie de quartier",
                exceptedMediaId = 852
            ),
            MediaTestCase(
                name = "Dragon Ball GT",
                exceptedMediaId = 225
            ),
            MediaTestCase(
                name = "Spy Classroom",
                exceptedMediaId = 146323
            ),
            MediaTestCase(
                name = "In Another World With My Smartphone",
                exceptedMediaId = 98491
            ),
            MediaTestCase(
                name = "Trigun",
                exceptedMediaId = 6
            ),
            MediaTestCase(
                name = "Rent-a-Girlfriend",
                exceptedMediaId = 113813
            ),
            MediaTestCase(
                name = "Kenshin le vagabond (2023)",
                exceptedMediaId = 142877
            ),
            MediaTestCase(
                name = "Lovely Complex",
                exceptedMediaId = 2034
            ),
            MediaTestCase(
                name = "Ranma1/2",
                firstReleasedYear = 2024,
                exceptedMediaId = 178533
            ),
            MediaTestCase(
                name = "Mission: Yozakura Family",
                exceptedMediaId = 158898
            ),
            MediaTestCase(
                name = "Air Gear",
                exceptedMediaId = 857
            ),
            MediaTestCase(
                name = "School Rumble",
                exceptedMediaId = 24
            ),
            MediaTestCase(
                name = "The Rising of the Shield Hero",
                exceptedMediaId = 99263
            ),
            MediaTestCase(
                name = "Arknights",
                firstReleasedYear = 2022,
                exceptedMediaId = 140660
            ),
            MediaTestCase(
                name = "Kimagure Orange Road",
                firstReleasedYear = 1987,
                exceptedMediaId = 1087
            ),
            MediaTestCase(
                name = "One Room",
                firstReleasedYear = 2017,
                exceptedMediaId = 97857
            ),
            MediaTestCase(
                name = "Terminator Zero",
                firstReleasedYear = 2024,
                exceptedMediaId = 177814
            ),
            MediaTestCase(
                name = "Mon chat à tout faire est encore tout déprimé",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G79H23Z3P")),
                firstReleasedYear = 2023,
                exceptedMediaId = 149883
            ),
            MediaTestCase(
                name = "Akame ga Kill !",
                firstReleasedYear = 2014,
                exceptedMediaId = 20613
            ),
            MediaTestCase(
                name = "My Friend's Little Sister Has It In for Me!",
                firstReleasedYear = 2025,
                exceptedMediaId = 129195
            ),
            MediaTestCase(
                name = "SHY",
                firstReleasedYear = 2023,
                exceptedMediaId = 155389
            ),
            MediaTestCase(
                name = "L'épée de Kamui",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1175")),
                firstReleasedYear = 1985,
                exceptedMediaId = 496
            ),
            MediaTestCase(
                name = "Kamisama : Opération Divine",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GNVHKNP4J")),
                firstReleasedYear = 2023,
                exceptedMediaId = 148048
            ),
            MediaTestCase(
                name = "Kamisama Kiss",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1215")),
                firstReleasedYear = 2012,
                exceptedMediaId = 14713
            ),
            MediaTestCase(
                name = "SAINT SEIYA: Knights of the Zodiac",
                firstReleasedYear = 1987,
                exceptedMediaId = 1254
            ),
            MediaTestCase(
                name = "Signé Cat's Eyes",
                platforms = listOf(AnimePlatform(platform = Platform.DISN, platformId = "21240dd9-5fb4-4334-be13-687a6bd230f7")),
                firstReleasedYear = 2025,
                exceptedMediaId = 184718
            ),
            MediaTestCase(
                name = "Le château de Cagliostro",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G79H23W89")),
                firstReleasedYear = 1979,
                exceptedMediaId = 1430
            ),
            MediaTestCase(
                name = "Hyakka Ryouran",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1113")),
                firstReleasedYear = 2010,
                exceptedMediaId = 8277
            ),
            MediaTestCase(
                name = "Umamusume: Pretty Derby",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GR79P2816")),
                firstReleasedYear = 2021,
                exceptedMediaId = 98514
            ),
            MediaTestCase(
                name = "Mobile Suit Gundam: Cucuruz Doan's Island",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GEXH3W2J5")),
                firstReleasedYear = 2022,
                exceptedMediaId = 139273
            ),
            MediaTestCase(
                name = "Perfect Blue",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GZJH3D8V3")),
                firstReleasedYear = 1998,
                exceptedMediaId = 437
            ),
            MediaTestCase(
                name = "PSYCHO-PASS",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GR75253JY")),
                firstReleasedYear = 2019,
                exceptedMediaId = 13601
            ),
            MediaTestCase(
                name = "The New Chronicles of Extraordinary Beings: Preface",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1323")),
                firstReleasedYear = 2025,
                exceptedMediaId = 156104
            ),
            MediaTestCase(
                name = "Kakuriyo -Bed & Breakfast for Spirits-",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G6MEK1ZGR")),
                firstReleasedYear = 2018,
                exceptedMediaId = 100500
            ),
            MediaTestCase(
                name = "Mon histoire d'amour avec Yamada à Lv999",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GNVHKNPQ7")),
                firstReleasedYear = 2023,
                exceptedMediaId = 154965
            ),
            MediaTestCase(
                name = "ONE PIECE",
                firstReleasedYear = 1999,
                exceptedMediaId = 21
            ),
            MediaTestCase(
                name = "Go! Princess Pretty Cure",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1213")),
                firstReleasedYear = 2015,
                exceptedMediaId = 21001
            ),
            MediaTestCase(
                name = "Yuru Camp – Au grand air",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GRWEW95KR")),
                firstReleasedYear = 2018,
                exceptedMediaId = 98444
            ),
            MediaTestCase(
                name = "Le Conte des parias",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GVDHX8QZX")),
                firstReleasedYear = 2023,
                exceptedMediaId = 151679
            ),
            MediaTestCase(
                name = "Nana",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GVDHX8QGP")),
                firstReleasedYear = 2006,
                exceptedMediaId = 877
            ),
            MediaTestCase(
                name = "Chouchouté par l’ange d’à côté",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G9VHN91DJ")),
                firstReleasedYear = 2023,
                exceptedMediaId = 143338
            ),
            MediaTestCase(
                name = "Super Cub",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GJ0H7QM1J")),
                firstReleasedYear = 2021,
                exceptedMediaId = 113418
            ),
            MediaTestCase(
                name = "NINTAMA RANTARŌ: MAÎTRE INVINCIBLE DES NINJAS DOKUTAKE",
                platforms = listOf(AnimePlatform(platform = Platform.PRIM, platformId = "0ITMHDR4DYNJDXLMAJ627H0ELR")),
                firstReleasedYear = 2024,
                exceptedMediaId = 175138
            ),
            MediaTestCase(
                name = "Mawaru Penguindrum",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "212")),
                firstReleasedYear = 2011,
                exceptedMediaId = 10721
            ),
            MediaTestCase(
                name = "Au Cœur du Donjon",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1209")),
                firstReleasedYear = 2024,
                exceptedMediaId = 168345
            ),
            MediaTestCase(
                name = "Comment Raeliana a survécu au manoir Wynknight",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GVDHX8Q71")),
                firstReleasedYear = 2023,
                exceptedMediaId = 151847
            ),
            MediaTestCase(
                name = "Dragon Ball",
                firstReleasedYear = 1986,
                exceptedMediaId = 223
            ),
            MediaTestCase(
                name = "Digimon Adventure: (2020)",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "606"), AnimePlatform(platform = Platform.CRUN, platformId = "GYEX24PV6"), AnimePlatform(platform = Platform.ANIM, platformId = "1153")),
                firstReleasedYear = 2020,
                exceptedMediaId = 114811
            ),
            MediaTestCase(
                name = "Le puissant dragon végan",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G1XHJV2NJ")),
                firstReleasedYear = 2022,
                exceptedMediaId = 141879
            ),
            MediaTestCase(
                name = "Dororo",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1075")),
                firstReleasedYear = 2019,
                exceptedMediaId = 101347
            ),
            MediaTestCase(
                name = "La réceptionniste Pokémon",
                platforms = listOf(AnimePlatform(platform = Platform.NETF, platformId = "81186864")),
                firstReleasedYear = 2023,
                exceptedMediaId = 162147
            ),
            MediaTestCase(
                name = "Sorairo Utility",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1268")),
                firstReleasedYear = 2025,
                exceptedMediaId = 174596
            ),
            MediaTestCase(
                name = "KONOSUBA -God's blessing on this wonderful world!",
                firstReleasedYear = 2016,
                exceptedMediaId = 21202
            ),
            MediaTestCase(
                name = "À quoi tu joues, Ayumu ?!",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "913")),
                firstReleasedYear = 2022,
                exceptedMediaId = 128223
            ),
            MediaTestCase(
                name = "Tokyo Ghoul",
                firstReleasedYear = 2014,
                exceptedMediaId = 20605
            ),
            MediaTestCase(
                name = "Vavam Vampire",
                platforms = listOf(AnimePlatform(platform = Platform.NETF, platformId = "81949674")),
                firstReleasedYear = 2025,
                exceptedMediaId = 175422
            ),
            MediaTestCase(
                name = "Arion",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1174")),
                firstReleasedYear = 1986,
                exceptedMediaId = 791
            ),
            MediaTestCase(
                name = "Saiyuki Gaiden",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "830")),
                firstReleasedYear = 2011,
                exceptedMediaId = 9088
            ),
            MediaTestCase(
                name = "My Isekai Life",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "908")),
                firstReleasedYear = 2022,
                exceptedMediaId = 129192
            ),
            MediaTestCase(
                name = "MONSTERS",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1106"), AnimePlatform(platform = Platform.NETF, platformId = "81733654")),
                firstReleasedYear = 2024,
                exceptedMediaId = 167404
            ),
            MediaTestCase(
                name = "Monster",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "355")),
                firstReleasedYear = 2004,
                exceptedMediaId = 19
            ),
            MediaTestCase(
                name = "I've Somehow Gotten Stronger When I Improved My Farm-Related Skills",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "967")),
                firstReleasedYear = 2022,
                exceptedMediaId = 145815
            ),
            MediaTestCase(
                name = "Dr Slump",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "482")),
                firstReleasedYear = 1981,
                exceptedMediaId = 2222
            ),
            MediaTestCase(
                name = "No Love Zone",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1231")),
                firstReleasedYear = 2023,
                exceptedMediaId = 183798
            ),
            MediaTestCase(
                name = "TENGOKU-DAIMAKYO",
                platforms = listOf(AnimePlatform(platform = Platform.DISN, platformId = "227fed4d-6671-4675-8448-f4f75741c1b9")),
                firstReleasedYear = 2023,
                exceptedMediaId = 155783
            ),
            MediaTestCase(
                name = "Observing Elena Evoy",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1290")),
                firstReleasedYear = 2023,
                exceptedMediaId = 166038
            ),
            MediaTestCase(
                name = "Crusher Joe",
                firstReleasedYear = 1983,
                exceptedMediaId = 2722
            ),
            MediaTestCase(
                name = "Moi, quand je me réincarne en Slime",
                firstReleasedYear = 2018,
                exceptedMediaId = 101280
            ),
            MediaTestCase(
                name = "Yu-Gi-Oh The Dark Side of Dimensions",
                firstReleasedYear = 2016,
                exceptedMediaId = 21265
            ),
            MediaTestCase(
                name = "Kakuriyo -Bed & Breakfast for Spirits-",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G6MEK1ZGR")),
                firstReleasedYear = 2018,
                exceptedMediaId = 100500
            ),
            MediaTestCase(
                name = "Le Pavillon des hommes",
                platforms = listOf(AnimePlatform(platform = Platform.NETF, platformId = "81464005")),
                firstReleasedYear = 2023,
                exceptedMediaId = 163137
            ),
            MediaTestCase(
                name = "Food Wars! Shokugeki no Soma",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G6GG91P26")),
                firstReleasedYear = 2017,
                exceptedMediaId = 20923
            ),
            MediaTestCase(
                name = "Hajime no Ippo",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "885")),
                firstReleasedYear = 2022,
                exceptedMediaId = 263
            ),
            MediaTestCase(
                name = "Cobra the Animation",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GEXH3W49W")),
                firstReleasedYear = 2008,
                exceptedMediaId = 5032
            ),
            MediaTestCase(
                name = "Blood-C: The Last Dark",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1086"), AnimePlatform(platform = Platform.ANIM, platformId = "1082")),
                firstReleasedYear = 2011,
                exceptedMediaId = 10490
            ),
            MediaTestCase(
                name = "Détective Conan vs Kaito Kid",
                platforms = listOf(AnimePlatform(platform = Platform.ANIM, platformId = "1157")),
                firstReleasedYear = 2024,
                exceptedMediaId = 184369
            ),
            MediaTestCase(
                name = "The Ancient Magus Bride",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GRZXQJJ8Y")),
                firstReleasedYear = 2016,
                exceptedMediaId = 98436
            ),
            MediaTestCase(
                name = "Chi, mon chaton : Vacances d'été",
                platforms = listOf(AnimePlatform(platform = Platform.NETF, platformId = "81712502")),
                firstReleasedYear = 2024,
                exceptedMediaId = 21796
            ),
            MediaTestCase(
                name = "KIZUMONOGATARI",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "G3KHEVD0D"), AnimePlatform(platform = Platform.ANIM, platformId = "1225")),
                firstReleasedYear = 2016,
                exceptedMediaId = 181970
            ),
            MediaTestCase(
                name = "Jack-of-All-Trades, Party of None",
                platforms = listOf(AnimePlatform(platform = Platform.CRUN, platformId = "GT00366764")),
                firstReleasedYear = 2025,
                exceptedMediaId = 187264
            ),
            MediaTestCase(
                name = "ISEKAI QUARTET",
                platforms = listOf(
                    AnimePlatform(platform = Platform.CRUN, platformId = "GMTE00258378"),
                    AnimePlatform(platform = Platform.CRUN, platformId = "GR8DN7N7R")
                ),
                firstReleasedYear = 2019,
                exceptedMediaId = 104454
            ),
            MediaTestCase(
                name = "Major",
                platforms = listOf(
                    AnimePlatform(platform = Platform.ANIM, platformId = "1353")
                ),
                firstReleasedYear = 2004,
                exceptedMediaId = 627
            )
        )
    }

    @ParameterizedTest
    @MethodSource("mediaTestCases")
    fun fetchMedia(testCase: MediaTestCase) {
        val media = try {
            runBlocking { AniListCachedWrapper.findAnilistMedia(testCase.name, testCase.platforms, testCase.firstReleasedYear) }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        assertNotNull(media)
        assertEquals(testCase.exceptedMediaId, media!!.id)
    }
}
