package fr.shikkanime.jobs.metadata

import fr.shikkanime.models.MetadataPlatform

/**
 * Manually verified metadata links, keyed by the stored normalized title.
 *
 * The automated matching covers the titles the external catalogs know; entries whose stored
 * title is a localized name the catalogs never heard of (French titles, for instance) are
 * pinned here after a manual check of the candidate entries. Each pin names the external
 * identifier verified by hand and the stored seasons it covers: a franchise split into one
 * entry per season gets one pin per season, a continuous catalog entry gets every season.
 */
object MetadataOverrides {
    /**
     * One verified external entry and the stored seasons it answers for.
     *
     * @param seasons stored seasons covered by the external entry; an empty set marks a
     * franchise reference: the entry gets an anime link but owns no stored episodes, so no
     * episode link is ever created for it.
     * @param id external identifier on the platform.
     * @param episodeOffset shift between the stored episode numbers and the external ones:
     * the external episode numbered `n` answers the stored episode `n - episodeOffset`.
     * Needed when the platform numbers episodes continuously across a franchise while the
     * metadata entry restarts at one (stored 130-154 against external 1-25 gives -129).
     */
    data class SeasonPin(
        val seasons: Set<Int>,
        val id: String,
        val episodeOffset: Int = 0
    )

    /**
     * Verified links keyed by stored normalized title.
     *
     * A platform mapped to an empty pin list is a verified absence: the title was checked by
     * hand and the platform holds no entry for it (unreleased local productions, for instance),
     * so the automatic matching must not guess one.
     */
    val anime: Map<String, Map<MetadataPlatform, List<SeasonPin>>> = mapOf(
        "A Certain Dark Item" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(1), "186742")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(1), "61014")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(1), "19159"))
        ),
        "A Returner's Magic Should Be Special" to mapOf(
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "163142"),
                SeasonPin(setOf(2), "172192")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "54852"),
                SeasonPin(setOf(2), "57612", episodeOffset = -12)
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "17944"),
                SeasonPin(setOf(2), "18345", episodeOffset = -12)
            )
        ),
        "A Wild Last Boss Appeared!" to mapOf(
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "180523"),
                SeasonPin(setOf(2), "204389")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "59644"),
                SeasonPin(setOf(2), "63140")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "18831"),
                SeasonPin(setOf(2), "19777")
            )
        ),
        "As a Reincarnated Aristocrat" to mapOf(
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "164702"),
                SeasonPin(emptySet(), "178434"),
                SeasonPin(setOf(3), "185756")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "55265"),
                SeasonPin(emptySet(), "59131"),
                SeasonPin(setOf(3), "60601")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "17552"),
                SeasonPin(emptySet(), "18026"),
                SeasonPin(emptySet(), "18707"),
                SeasonPin(setOf(3), "19070")
            )
        ),
        "Dandivine" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(1), "213068")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(1), "64344")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(1), "20081"))
        ),
        "Dreamland" to mapOf(
            MetadataPlatform.ANILIST to emptyList(),
            MetadataPlatform.MYANIMELIST to emptyList(),
            MetadataPlatform.ANIDB to emptyList()
        ),
        "Détective Conan" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(31), "235")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(31), "235")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(31), "266"))
        ),
        "Even the Student Council Has Its Holes!" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(1), "191656")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(1), "61578")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(1), "19308"))
        ),
        "FX Fighter Kurumi-chan" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(1), "206401")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(1), "63337")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(1), "19847"))
        ),
        "HOTEL INHUMANS" to mapOf(
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "184034"),
                SeasonPin(setOf(2), "199426")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "60260"),
                SeasonPin(setOf(2), "62590")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "18973"),
                SeasonPin(setOf(2), "19621")
            )
        ),
        "Le Collège Noir" to mapOf(
            MetadataPlatform.ANILIST to emptyList(),
            MetadataPlatform.MYANIMELIST to emptyList(),
            MetadataPlatform.ANIDB to emptyList()
        ),
        "Les Carnets de l'apothicaire" to mapOf(
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "161645"),
                SeasonPin(emptySet(), "176301"),
                SeasonPin(emptySet(), "200927"),
                SeasonPin(setOf(3), "195516")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "54492"),
                SeasonPin(emptySet(), "58514"),
                SeasonPin(emptySet(), "62841"),
                SeasonPin(setOf(3), "61987", episodeOffset = -48)
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "17870"),
                SeasonPin(emptySet(), "18562"),
                SeasonPin(emptySet(), "19671"),
                SeasonPin(setOf(3), "19444", episodeOffset = -48)
            )
        ),
        "Les Noces des Lucioles" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(1), "205909")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(1), "63293")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(1), "19823"))
        ),
        "Link Click" to mapOf(
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "126403"),
                SeasonPin(emptySet(), "136484"),
                SeasonPin(setOf(5), "191832")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "44074"),
                SeasonPin(emptySet(), "49413"),
                SeasonPin(setOf(5), "61607")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "16290"),
                SeasonPin(emptySet(), "16531"),
                SeasonPin(setOf(5), "19882")
            )
        ),
        "Magic Knight Rayearth" to mapOf(
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "435"),
                SeasonPin(emptySet(), "1563"),
                SeasonPin(setOf(1), "178868")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "435"),
                SeasonPin(emptySet(), "1563"),
                SeasonPin(setOf(1), "59204")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "127"),
                SeasonPin(setOf(1), "18729")
            )
        ),
        "Major" to mapOf(
            // Only season 6 is stored: seasons 1 to 5 are franchise references.
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "627"),
                SeasonPin(emptySet(), "558"),
                SeasonPin(emptySet(), "1842"),
                SeasonPin(emptySet(), "3226"),
                SeasonPin(emptySet(), "5028"),
                SeasonPin(setOf(6), "7655")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "627"),
                SeasonPin(emptySet(), "558"),
                SeasonPin(emptySet(), "1842"),
                SeasonPin(emptySet(), "3226"),
                SeasonPin(emptySet(), "5028"),
                SeasonPin(setOf(6), "7655", episodeOffset = -129)
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "2168"),
                SeasonPin(emptySet(), "3423"),
                SeasonPin(emptySet(), "4557"),
                SeasonPin(emptySet(), "5316"),
                SeasonPin(emptySet(), "6013"),
                SeasonPin(setOf(6), "7236", episodeOffset = -129)
            )
        ),
        "My Hero Academia" to mapOf(
            // Seasons 1 to 6 are franchise references: the stored seasons are 7 to 9.
            // Season 7 holds the "Memories" recap specials: only MyAnimeList catalogues them.
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "31964"),
                SeasonPin(emptySet(), "33486"),
                SeasonPin(emptySet(), "36456"),
                SeasonPin(emptySet(), "38408"),
                SeasonPin(emptySet(), "41587"),
                SeasonPin(emptySet(), "49918"),
                SeasonPin(setOf(7), "57519"),
                SeasonPin(setOf(8), "54789", episodeOffset = -138),
                SeasonPin(setOf(9), "60098", episodeOffset = -159)
            ),
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "21459"),
                SeasonPin(emptySet(), "21856"),
                SeasonPin(emptySet(), "100166"),
                SeasonPin(emptySet(), "104276"),
                SeasonPin(emptySet(), "117193"),
                SeasonPin(emptySet(), "139630"),
                SeasonPin(setOf(8), "163139"),
                SeasonPin(setOf(9), "182896")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "11739"),
                SeasonPin(emptySet(), "12233"),
                SeasonPin(emptySet(), "13485"),
                SeasonPin(emptySet(), "14416"),
                SeasonPin(emptySet(), "15484"),
                SeasonPin(emptySet(), "16793"),
                SeasonPin(setOf(8), "17931", episodeOffset = -138),
                SeasonPin(setOf(9), "18921", episodeOffset = -159)
            )
        ),
        "Overgeared" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(1), "212888")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(1), "64340")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(1), "20077"))
        ),
        "PSYREN" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(1), "204011")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(1), "63098")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(1), "19765"))
        ),
        "Quatre frères pour moi !" to mapOf(
            MetadataPlatform.ANILIST to emptyList(),
            MetadataPlatform.MYANIMELIST to emptyList(),
            MetadataPlatform.ANIDB to emptyList()
        ),
        "Reincarnated as a Sword" to mapOf(
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "139587"),
                SeasonPin(setOf(2), "159042")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "49891"),
                SeasonPin(setOf(2), "53913")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "16785"),
                SeasonPin(setOf(2), "17789")
            )
        ),
        "Rilakkuma" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(1), "183231")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(1), "60153")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(1), "18936"))
        ),
        "Sergent Keroro" to mapOf(
            // The 2004 series and its 2014 season are earlier entries of the same franchise.
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "516"),
                SeasonPin(emptySet(), "20543"),
                SeasonPin(setOf(1), "216557")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "516"),
                SeasonPin(emptySet(), "21671"),
                SeasonPin(setOf(1), "63157")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "1552"),
                SeasonPin(emptySet(), "10306"),
                SeasonPin(setOf(1), "20085")
            )
        ),
        "TOUGEN ANKI" to mapOf(
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "177474"),
                SeasonPin(setOf(2), "204650")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "58811"),
                SeasonPin(setOf(2), "63181")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "18615"),
                SeasonPin(setOf(2), "19795")
            )
        ),
        "The Exiled Heavy Knight Knows How to Game the System" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(1), "180136")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(1), "59741")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(1), "18854"))
        ),
        "The Iceblade Sorcerer Shall Rule the World" to mapOf(
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "148116"),
                SeasonPin(setOf(2), "212503")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "51711"),
                SeasonPin(setOf(2), "64254")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "17341"),
                SeasonPin(setOf(2), "20065")
            )
        ),
        "The Prince of Tennis" to mapOf(
            // The U-17 World Cup run is split into three entries on every platform.
            MetadataPlatform.ANILIST to listOf(
                SeasonPin(emptySet(), "22"),
                SeasonPin(emptySet(), "11371"),
                SeasonPin(emptySet(), "140187"),
                SeasonPin(emptySet(), "165810"),
                SeasonPin(setOf(8), "199068")
            ),
            MetadataPlatform.MYANIMELIST to listOf(
                SeasonPin(emptySet(), "22"),
                SeasonPin(emptySet(), "11371"),
                SeasonPin(emptySet(), "50099"),
                SeasonPin(emptySet(), "55570"),
                SeasonPin(setOf(8), "62534")
            ),
            MetadataPlatform.ANIDB to listOf(
                SeasonPin(emptySet(), "56"),
                SeasonPin(emptySet(), "8601"),
                SeasonPin(emptySet(), "16878"),
                SeasonPin(emptySet(), "18054"),
                SeasonPin(setOf(8), "20348")
            )
        ),
        "Welcome to Yu-hachi's House!" to mapOf(
            MetadataPlatform.ANILIST to listOf(SeasonPin(setOf(1), "214593")),
            MetadataPlatform.MYANIMELIST to listOf(SeasonPin(setOf(1), "64717")),
            MetadataPlatform.ANIDB to listOf(SeasonPin(setOf(1), "20463"))
        )
    )
}
