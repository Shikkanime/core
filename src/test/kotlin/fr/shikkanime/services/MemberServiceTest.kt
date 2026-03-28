package fr.shikkanime.services

import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Member
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
}
