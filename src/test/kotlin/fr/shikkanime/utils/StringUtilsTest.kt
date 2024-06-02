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
            "Captain Tsubasa Saison 2, Junior Youth Arc" to "Captain Tsubasa, Junior Youth Arc",
            "SPY x FAMILY" to "SPY x FAMILY",
            "The Strongest Tank's Labyrinth Raids -A Tank with a Rare 9999 Resistance Skill Got Kicked from the Hero's Party-" to "The Strongest Tank's Labyrinth Raids",
            "Firefighter Daigo: Rescuer in Orange" to "Firefighter Daigo: Rescuer in Orange",
            "MASHLE: MAGIC AND MUSCLES" to "MASHLE: MAGIC AND MUSCLES",
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
            "Reborn as a Vending Machine, I Now Wander the Dungeon" to "Reborn as a Vending Machine",
            "BIRDIE WING -Golf Girls' Story-" to "BIRDIE WING",
            "Urusei Yatsura (2022)" to "Urusei Yatsura",
            "Cherry Magic! Thirty Years of Virginity Can Make You a Wizard?!" to "Cherry Magic",
            "KONOSUBA -God's blessing on this wonderful world!" to "KONOSUBA",
            "Moi, quand je me réincarne en Slime" to "Moi, quand je me réincarne en Slime",
            "I Was Reincarnated as the 7th Prince so I Can Take My Time Perfecting My Magical Ability" to "I Was Reincarnated as the 7th Prince",
            "Mushoku Tensei: Jobless Reincarnation" to "Mushoku Tensei: Jobless Reincarnation",
            "Yuru Camp – Au grand air" to "Yuru Camp – Au grand air",
            "Studio Apartment, Good Lighting, Angel Included" to "Studio Apartment, Good Lighting, Angel Included",
            "Je survivrai grâce aux potions !" to "Je survivrai grâce aux potions !",
            "Rent-a-Girlfriend" to "Rent-a-Girlfriend",
            "After-school Hanako-kun" to "After-school Hanako-kun",
            "Kaguya-sama: Love Is War" to "Kaguya-sama: Love Is War",
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
            "Kaguya-sama: Love Is War" to "KaguyaSamaLoveIsWar",
            "MASHLE: MAGIC AND MUSCLES" to "MashleMagicAndMuscles",
            "Yuru Camp – Au grand air" to "YuruCampAuGrandAir"
        )

        list.forEach { (input, expected) ->
            assertEquals(expected, StringUtils.getHashtag(StringUtils.getShortName(input)))
        }
    }
}