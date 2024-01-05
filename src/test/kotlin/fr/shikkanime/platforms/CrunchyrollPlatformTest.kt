package fr.shikkanime.platforms

import fr.shikkanime.caches.CountryCodeAnimeIdKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.Constant
import jakarta.inject.Inject
import java.io.File
import java.time.ZonedDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.expect

class CrunchyrollPlatformTest {
    @Inject
    lateinit var platform: CrunchyrollPlatform

    @BeforeTest
    fun setUp() {
        Constant.injector.injectMembers(this)

        platform.loadConfiguration()
        platform.configuration!!.availableCountries.add(CountryCode.FR)
    }

    @Test
    fun fetchEpisodes() {
        val s = "2023-12-11T18:00:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.simulcasts[CountryCode.FR] = setOf(
            "16bit sensation: another layer",
            "a girl & her guard dog",
            "a playthrough of a certain dude's vrmmo life",
            "a returner's magic should be special",
            "after-school hanako-kun",
            "all saints street",
            "arknights",
            "berserk of gluttony",
            "bullbuster",
            "butareba -the story of a man turned into a pig-",
            "captain tsubasa saison 2, junior youth arc",
            "dead mount death play",
            "dr. stone",
            "firefighter daigo: rescuer in orange",
            "frieren",
            "girlfriend, girlfriend",
            "heaven official's blessing",
            "hypnosismic -division rap battle- rhyme anima",
            "i'm giving the disgraced noble lady i rescued a crash course in naughtiness",
            "i'm in love with the villainess",
            "idolish7",
            "je survivrai grâce aux potions !",
            "jujutsu kaisen",
            "kamierabi god.app",
            "kawagoe boys sing -now or never-",
            "kenshin le vagabond (2023)",
            "kizuna no allele",
            "l'attaque des titans",
            "la valkyrie aux cheveux de jais",
            "les 100 petites amies qui t'aiiiment à en mourir",
            "les carnets de l'apothicaire",
            "les quatre frères yuzuki",
            "let me check the walkthrough first",
            "mf ghost",
            "migi&dali",
            "moi, quand je me réincarne en slime",
            "my new boss is goofy",
            "one piece",
            "our dating story: the experienced you and the inexperienced me",
            "overtake!",
            "paradox live the animation",
            "power of hope ~precure full bloom~",
            "reign of the seven spellblades",
            "ron kamonohashi: deranged detective",
            "sasaki and miyano",
            "shadowverse",
            "shangri-la frontier",
            "shy",
            "spy x family",
            "stardust telepath",
            "tearmoon empire",
            "tenchi muyo! gxp paradise starting",
            "the ancient magus bride",
            "the faraway paladin",
            "the idolm@ster million live!",
            "the kingdoms of ruin",
            "the rising of the shield hero",
            "the saint's magic power is omnipotent",
            "umamusume: pretty derby",
            "under ninja",
            "witch family!",
            "am i actually the strongest?",
            "atelier ryza: ever darkness & the secret hideout the animation",
            "ayaka",
            "ayakashi triangle",
            "bang dream! it's mygo!!!!!",
            "bungo stray dogs",
            "cardfight!! vanguard overdress",
            "classroom for heroes",
            "fate/strange fake -whispers of dawn-",
            "horimiya",
            "la princesse & la bête",
            "liar, liar",
            "link click",
            "malevolent spirits: mononogatari",
            "masamune-kun's revenge",
            "mon chat à tout faire est encore tout déprimé",
            "mushoku tensei: jobless reincarnation",
            "my tiny senpai",
            "my unique skill makes me op even at level 1",
            "nier:automata ver1.1a",
            "reborn as a vending machine, i now wander the dungeon",
            "rent-a-girlfriend",
            "sainte cecilia et le pasteur lawrence",
            "sugar apple fairy tale",
            "sweet reincarnation",
            "sword art online -fulldive-",
            "tenpuru : on ne vit pas de solitude et d'eau fraîche",
            "the devil is a part-timer!",
            "the duke of death and his maid",
            "the gene of ai",
            "the girl i like forgot her glasses",
            "the great cleric",
            "the misfit of demon king academy",
            "theatre of darkness: yamishibai",
            "undead murder farce",
            "zom 100: bucket list of the dead"
        )

        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "im-in-love-with-the-villainess")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/3f84cdad5e39d6d35ffc2b127c0509c7.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "shy")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/d117d6da529adb8ee4d972af79024365.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "dead-mount-death-play")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/3c9bebb79d5abf889928349df37a42b0.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "kawagoe-boys-sing-now-or-never-")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/cfdf23b73e919c35c4c6168d0939907a.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "ron-kamonohashis-forbidden-deductions")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/a755fe3f774aedeb7115a6d0419c5fe7.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "stardust-telepath")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/9b1a36e0ebf66dc87f4fa8f623bf1ec5.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "shangri-la-frontier")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/a2f948157077e3d65471329d9dd43be1.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "mf-ghost")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/00c8ed6414e0ef37dbacf36bec86565a.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "berserk-of-gluttony")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/a5ebfaaf02d69d1c0b254f4a49c43082.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "overtake")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/150047844fbb4c07a3c57b5def7b42b0.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(
            CountryCode.FR,
            "the-family-circumstances-of-the-irregular-witch"
        )] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/43fce24dc917ee4e415aeae5231be54e.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(
            CountryCode.FR,
            "the-100-girlfriends-who-really-really-really-really-really-love-you"
        )] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/0ab1a205c9c541ad87cefdbe7fd23390.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "captain-tsubasa-junior-youth-arc")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/f0dcc54fec6d201f5a2c7abd01d56e09.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "the-idolmster-million-live")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/550acd8c749fa3820d4f75868d769e71.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "one-piece")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/f154230aab3191aba977f337d392f812.jpe")
        platform.animeInfoCache[CountryCodeAnimeIdKeyCache(CountryCode.FR, "the-apothecary-diaries")] =
            CrunchyrollPlatform.CrunchyrollAnimeContent("https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/af8f1de4c1b2d5345294490a45fcb22d.jpe")

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/rss-${s.replace(':', '-')}.xml")?.file
                    ?: throw Exception("File not found")
            )
        )

        println(episodes)

        assert(episodes.isNotEmpty())
        expect(18) { episodes.size }

        expect("I'm in Love with the Villainess") { episodes[0].anime?.name }
        expect("I'm in Love with the Villainess") { episodes[1].anime?.name }
        expect("SHY") { episodes[2].anime?.name }
        expect("Dead Mount Death Play") { episodes[3].anime?.name }
    }
}