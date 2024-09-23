package fr.shikkanime.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringUtilsTest {

    @Test
    fun getShortName() {
        val list = listOf(
            "High Card" to "High Card",
            "Banished from the Hero's Party, I Decided to Live a Quiet Life in the Countryside" to "Banished from the Hero's Party",
            "7th Time Loop: The Villainess Enjoys a Carefree Life Married to Her Worst Enemy!" to "7th Time Loop",
            "Shangri-La Frontier" to "Shangri-La Frontier",
            "Captain Tsubasa Saison 2, Junior Youth Arc" to "Captain Tsubasa",
            "SPY x FAMILY" to "SPY x FAMILY",
            "The Strongest Tank's Labyrinth Raids -A Tank with a Rare 9999 Resistance Skill Got Kicked from the Hero's Party-" to "The Strongest Tank's Labyrinth Raids",
            "Firefighter Daigo: Rescuer in Orange" to "Firefighter Daigo",
            "MASHLE: MAGIC AND MUSCLES" to "MASHLE",
            "My Instant Death Ability Is So Overpowered, No One in This Other World Stands a Chance Against Me!" to "My Instant Death Ability Is So Overpowered",
            "Bottom-Tier Character Tomozaki" to "Bottom-Tier Character Tomozaki",
            "Classroom of the Elite" to "Classroom of the Elite",
            "Gloutons & Dragons" to "Gloutons & Dragons",
            "Protocol: Rain" to "Protocol: Rain",
            "B-PROJECT Passion*Love Call" to "B-PROJECT Passion*Love Call",
            "Butareba -The Story of a Man Turned into a Pig-" to "Butareba",
            "Our Dating Story: The Experienced You and The Inexperienced Me" to "Our Dating Story",
            "HYPNOSISMIC -Division Rap Battle- Rhyme Anima" to "HYPNOSISMIC Rhyme Anima",
            "Fate/strange Fake -Whispers of Dawn-" to "Fate/strange Fake",
            "NieR:Automata Ver1.1a" to "NieR:Automata Ver1.1a",
            "Reborn as a Vending Machine, I Now Wander the Dungeon" to "Reborn as a Vending Machine, I Now Wander the Dungeon",
            "BIRDIE WING -Golf Girls' Story-" to "BIRDIE WING",
            "Urusei Yatsura (2022)" to "Urusei Yatsura",
            "Cherry Magic! Thirty Years of Virginity Can Make You a Wizard?!" to "Cherry Magic",
            "KONOSUBA -God's blessing on this wonderful world!" to "KONOSUBA",
            "Moi, quand je me réincarne en Slime" to "Moi, quand je me réincarne en Slime",
            "I Was Reincarnated as the 7th Prince so I Can Take My Time Perfecting My Magical Ability" to "I Was Reincarnated as the 7th Prince",
            "Mushoku Tensei: Jobless Reincarnation" to "Mushoku Tensei: Jobless Reincarnation",
            "Yuru Camp – Au grand air" to "Yuru Camp",
            "Studio Apartment, Good Lighting, Angel Included" to "Studio Apartment, Good Lighting, Angel Included",
            "Je survivrai grâce aux potions !" to "Je survivrai grâce aux potions !",
            "Rent-a-Girlfriend" to "Rent-a-Girlfriend",
            "After-school Hanako-kun" to "After-school Hanako-kun",
            "Kaguya-sama: Love Is War" to "Kaguya-sama",
            "DanMachi - La Légende des Familias" to "DanMachi",
            "Demon Slayer - Le village des forgerons" to "Demon Slayer",
            "Dragon Quest - The Adventures of Dai" to "Dragon Quest",
            "Si je suis la Vilaine, autant mater le boss final" to "Si je suis la Vilaine, autant mater le boss final",
            "Reborn to Master the Blade: From Hero-King to Extraordinary Squire" to "Reborn to Master the Blade",
            "Stand My Heroes: Piece of Truth" to "Stand My Heroes",
            "CARDFIGHT!! VANGUARD overDress" to "CARDFIGHT!! VANGUARD overDress",
            "Re:ZERO –Starting Life in Another World–" to "Re:ZERO",
            "Arifureta: From Commonplace to World's Strongest" to "Arifureta",
            "Digimon Adventure: (2020)" to "Digimon Adventure",
            "Kenshin le vagabond (2023)" to "Kenshin le vagabond",
            "Strike the Blood - Valkyria no Ôkoku-hen" to "Strike the Blood",
            "Strike the Blood II" to "Strike the Blood",
            "Overlord IV" to "Overlord",
            "The Seven Deadly Sins: Four Knights of the Apocalypse" to "The Seven Deadly Sins",
            "86 EIGHTY-SIX" to "86 EIGHTY-SIX",
            "JORAN THE PRINCESS OF SNOW AND BLOOD" to "JORAN THE PRINCESS OF SNOW AND BLOOD",
            "MIX" to "MIX",
            "Cyberpunk: Edgerunners" to "Cyberpunk: Edgerunners",
        )

        list.forEach { (input, expected) ->
            assertEquals(expected, StringUtils.getShortName(input))
        }
    }

    @Test
    fun toSlug() {
        val list = listOf(
            "Gloutons & Dragons" to "gloutons-dragons",
            "Moi, quand je me réincarne en Slime" to "moi-quand-je-me-reincarne-en-slime",
            "I Was Reincarnated as the 7th Prince so I Can Take My Time Perfecting My Magical Ability" to "i-was-reincarnated-as-the-7th-prince",
            "Studio Apartment, Good Lighting, Angel Included" to "studio-apartment-good-lighting-angel-included",
            "Je survivrai grâce aux potions !" to "je-survivrai-grace-aux-potions",
            "Rent-a-Girlfriend" to "rent-a-girlfriend",
            "Re:Monster" to "re-monster",
            "NieR:Automata Ver1.1a" to "nier-automata-ver1-1a",
            "An Archdemon's Dilemma: How to Love Your Elf Bride" to "an-archdemons-dilemma",
            "Protocol: Rain" to "protocol-rain",
            "Spice and Wolf: MERCHANT MEETS THE WISE WOLF" to "spice-and-wolf",
            "KONOSUBA -God's blessing on this wonderful world!" to "konosuba",
            "Fate/strange Fake -Whispers of Dawn-" to "fate-strange-fake",
            "After-school Hanako-kun" to "after-school-hanako-kun",
            "The Strongest Tank's Labyrinth Raids -A Tank with a Rare 9999 Resistance Skill Got Kicked from the Hero's Party-" to "the-strongest-tanks-labyrinth-raids",
            "'Tis Time for \"Torture,\" Princess" to "tis-time-for-torture-princess",
            "X&Y" to "x-y",
            "DanMachi - La Légende des Familias" to "danmachi",
            "Demon Slayer - Le village des forgerons" to "demon-slayer",
            "Dragon Quest - The Adventures of Dai" to "dragon-quest",
            "Au Cœur du Donjon" to "au-coeur-du-donjon",
            "THE IDOLM@STER Million Live!" to "the-idolmaster-million-live",
            "MIX" to "mix",
        )

        list.forEach { (input, expected) ->
            assertEquals(expected, StringUtils.toSlug(StringUtils.getShortName(input)))
        }
    }

    @Test
    fun getHashtag() {
        val list = listOf(
            "Gloutons & Dragons" to "GloutonsDragons",
            "Moi, quand je me réincarne en Slime" to "MoiQuandJeMeRéincarneEnSlime",
            "I Was Reincarnated as the 7th Prince so I Can Take My Time Perfecting My Magical Ability" to "IWasReincarnatedAsThe7thPrince",
            "Studio Apartment, Good Lighting, Angel Included" to "StudioApartmentGoodLightingAngelIncluded",
            "Je survivrai grâce aux potions !" to "JeSurvivraiGrâceAuxPotions",
            "Rent-a-Girlfriend" to "RentAGirlfriend",
            "Re:Monster" to "ReMonster",
            "NieR:Automata Ver1.1a" to "NierAutomataVer11a",
            "An Archdemon's Dilemma: How to Love Your Elf Bride" to "AnArchdemonsDilemma",
            "Protocol: Rain" to "ProtocolRain",
            "Spice and Wolf: MERCHANT MEETS THE WISE WOLF" to "SpiceAndWolf",
            "KONOSUBA -God's blessing on this wonderful world!" to "Konosuba",
            "Fate/strange Fake -Whispers of Dawn-" to "FateStrangeFake",
            "After-school Hanako-kun" to "AfterSchoolHanakoKun",
            "The Strongest Tank's Labyrinth Raids -A Tank with a Rare 9999 Resistance Skill Got Kicked from the Hero's Party-" to "TheStrongestTanksLabyrinthRaids",
            "'Tis Time for \"Torture,\" Princess" to "TisTimeForTorturePrincess",
            "X&Y" to "XY",
            "Kaguya-sama: Love Is War" to "KaguyaSama",
            "MASHLE: MAGIC AND MUSCLES" to "Mashle",
            "Yuru Camp – Au grand air" to "YuruCamp",
            "Jellyfish Can't Swim in the Night" to "JellyfishCantSwimInTheNight",
            "Au Cœur du Donjon" to "AuCoeurDuDonjon",
            "THE IDOLM@STER Million Live!" to "TheIdolmasterMillionLive",
            "Overlord IV" to "Overlord",
        )

        list.forEach { (input, expected) ->
            assertEquals(expected, StringUtils.getHashtag(StringUtils.getShortName(input)))
        }
    }
}