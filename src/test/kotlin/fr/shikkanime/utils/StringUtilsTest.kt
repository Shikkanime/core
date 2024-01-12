package fr.shikkanime.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringUtilsTest {

    @Test
    fun getShortName() {
        assertEquals("High Card", StringUtils.getShortName("High Card"))
        assertEquals(
            "Banished from the Hero's Party",
            StringUtils.getShortName("Banished from the Hero's Party, I Decided to Live a Quiet Life in the Countryside")
        )
        assertEquals(
            "7th Time Loop",
            StringUtils.getShortName("7th Time Loop: The Villainess Enjoys a Carefree Life Married to Her Worst Enemy!")
        )
        assertEquals("Shangri-La Frontier", StringUtils.getShortName("Shangri-La Frontier"))
        assertEquals("Captain Tsubasa", StringUtils.getShortName("Captain Tsubasa Saison 2, Junior Youth Arc"))
        assertEquals("SPY x FAMILY", StringUtils.getShortName("SPY x FAMILY"))
        assertEquals(
            "The Strongest Tank's Labyrinth Raids",
            StringUtils.getShortName("The Strongest Tank's Labyrinth Raids -A Tank with a Rare 9999 Resistance Skill Got Kicked from the Hero's Party-")
        )
        assertEquals("Firefighter Daigo", StringUtils.getShortName("Firefighter Daigo: Rescuer in Orange"))
        assertEquals("MASHLE", StringUtils.getShortName("MASHLE: MAGIC AND MUSCLES"))
        assertEquals(
            "My Instant Death Ability Is So Overpowered",
            StringUtils.getShortName("My Instant Death Ability Is So Overpowered, No One in This Other World Stands a Chance Against Me!")
        )
        assertEquals("Bottom-Tier Character Tomozaki", StringUtils.getShortName("Bottom-Tier Character Tomozaki"))
        assertEquals("Classroom of the Elite", StringUtils.getShortName("Classroom of the Elite"))
        assertEquals("Gloutons & Dragons", StringUtils.getShortName("Gloutons & Dragons"))
        assertEquals("Protocol: Rain", StringUtils.getShortName("Protocol: Rain"))
        assertEquals("B-PROJECT Passion*Love Call", StringUtils.getShortName("B-PROJECT Passion*Love Call"))
        assertEquals("Butareba", StringUtils.getShortName("Butareba -The Story of a Man Turned into a Pig-"))
        assertEquals(
            "Our Dating Story",
            StringUtils.getShortName("Our Dating Story: The Experienced You and The Inexperienced Me")
        )
        assertEquals(
            "HYPNOSISMIC Rhyme Anima",
            StringUtils.getShortName("HYPNOSISMIC -Division Rap Battle- Rhyme Anima")
        )
        assertEquals("Fate/strange Fake", StringUtils.getShortName("Fate/strange Fake -Whispers of Dawn-"))
        assertEquals("NieR:Automata Ver1.1a", StringUtils.getShortName("NieR:Automata Ver1.1a"))
        assertEquals(
            "Reborn as a Vending Machine",
            StringUtils.getShortName("Reborn as a Vending Machine, I Now Wander the Dungeon")
        )
        assertEquals("BIRDIE WING", StringUtils.getShortName("BIRDIE WING -Golf Girls' Story-"))
        assertEquals("Urusei Yatsura", StringUtils.getShortName("Urusei Yatsura (2022)"))
        assertEquals(
            "Cherry Magic",
            StringUtils.getShortName("Cherry Magic! Thirty Years of Virginity Can Make You a Wizard?!")
        )
    }
}