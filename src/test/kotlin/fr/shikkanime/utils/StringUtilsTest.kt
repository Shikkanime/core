package fr.shikkanime.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringUtilsTest {

    @Test
    fun getShortName() {
        val list = listOf(
            "High Card" to "High Card",
            "Banished from the Hero's Party" to "Banished from the Hero's Party, I Decided to Live a Quiet Life in the Countryside",
            "7th Time Loop" to "7th Time Loop: The Villainess Enjoys a Carefree Life Married to Her Worst Enemy!",
            "Shangri-La Frontier" to "Shangri-La Frontier",
            "Captain Tsubasa" to "Captain Tsubasa Saison 2, Junior Youth Arc",
            "SPY x FAMILY" to "SPY x FAMILY",
            "The Strongest Tank's Labyrinth Raids" to "The Strongest Tank's Labyrinth Raids -A Tank with a Rare 9999 Resistance Skill Got Kicked from the Hero's Party-",
            "Firefighter Daigo" to "Firefighter Daigo: Rescuer in Orange",
            "MASHLE" to "MASHLE: MAGIC AND MUSCLES",
            "My Instant Death Ability Is So Overpowered" to "My Instant Death Ability Is So Overpowered, No One in This Other World Stands a Chance Against Me!",
            "Bottom-Tier Character Tomozaki" to "Bottom-Tier Character Tomozaki",
            "Classroom of the Elite" to "Classroom of the Elite",
            "Gloutons & Dragons" to "Gloutons & Dragons",
            "Protocol: Rain" to "Protocol: Rain",
            "B-PROJECT Passion*Love Call" to "B-PROJECT Passion*Love Call",
            "Butareba" to "Butareba -The Story of a Man Turned into a Pig-",
            "Our Dating Story" to "Our Dating Story: The Experienced You and The Inexperienced Me",
            "HYPNOSISMIC Rhyme Anima" to "HYPNOSISMIC -Division Rap Battle- Rhyme Anima",
            "Fate/strange Fake" to "Fate/strange Fake -Whispers of Dawn-",
            "NieR:Automata Ver1.1a" to "NieR:Automata Ver1.1a",
            "Reborn as a Vending Machine" to "Reborn as a Vending Machine, I Now Wander the Dungeon",
            "BIRDIE WING" to "BIRDIE WING -Golf Girls' Story-",
            "Urusei Yatsura" to "Urusei Yatsura (2022)",
            "Cherry Magic" to "Cherry Magic! Thirty Years of Virginity Can Make You a Wizard?!",
            "KONOSUBA" to "KONOSUBA -God's blessing on this wonderful world!"
        )

        list.forEach { (expected, input) ->
            assertEquals(expected, StringUtils.getShortName(input))
        }
    }

    @Test
    fun toSlug() {
        val list = listOf(
            "Gloutons & Dragons" to "gloutons-dragons",
        )

        list.forEach { (input, expected) ->
            assertEquals(expected, StringUtils.toSlug(input))
        }
    }
}