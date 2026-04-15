package fr.shikkanime.services

import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MemberServiceTest : AbstractTest() {

    @Test
    fun disassociateEmail() {
        val member = Member(
            username = "testuser",
            email = "test@example.com",
            encryptedPassword = byteArrayOf()
        )
        val saved = memberService.save(member)

        val savedMember = memberService.find(saved.uuid)
        assertNotNull(savedMember)
        assertEquals("test@example.com", savedMember!!.email)

        memberService.disassociateEmail(saved.uuid!!)

        val updatedMember = memberService.find(saved.uuid)
        assertNotNull(updatedMember)
        assertNull(updatedMember!!.email)
    }

    @Test
    fun deleteGDPR() {
        val member = memberService.save(
            Member(
                username = "testuser",
                email = "test@example.com",
                encryptedPassword = byteArrayOf()
            )
        )
        val memberUuid = member.uuid!!

        val anime = animeService.save(Anime(countryCode = CountryCode.FR, name = "Test Anime", slug = "test-anime"))
        val episode = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1
            )
        )

        memberFollowAnimeService.save(MemberFollowAnime(member = member, anime = anime))
        memberFollowEpisodeService.save(MemberFollowEpisode(member = member, episode = episode))
        traceActionService.createTraceAction(member, TraceAction.Action.LOGIN)

        assertFalse(memberFollowAnimeService.findAllByMember(memberUuid).isEmpty())
        assertFalse(memberFollowEpisodeService.findAllByMember(memberUuid).isEmpty())
        assertFalse(traceActionService.findAllByEntityUuid(memberUuid).isEmpty())

        memberService.delete(member)

        assertNull(memberService.find(memberUuid))
        assertTrue(memberFollowAnimeService.findAllByMember(memberUuid).isEmpty())
        assertTrue(memberFollowEpisodeService.findAllByMember(memberUuid).isEmpty())
        assertTrue(traceActionService.findAllByEntityUuid(memberUuid).isEmpty())
    }
}
