package com.salvarez.miboveda.domain

import com.salvarez.miboveda.domain.model.PasswordStrength
import com.salvarez.miboveda.domain.model.evaluatePasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordStrengthTest {
    private fun strength(password: String) = evaluatePasswordStrength(password.toCharArray())

    @Test
    fun `short passwords are weak`() = assertEquals(PasswordStrength.WEAK, strength("Ab1!"))

    @Test
    fun `long but single class passwords are weak`() =
        assertEquals(PasswordStrength.WEAK, strength("abcdefghijklmnop"))

    @Test
    fun `twelve characters with two classes is medium`() =
        assertEquals(PasswordStrength.MEDIUM, strength("abcdefgh1234"))

    @Test
    fun `sixteen characters with three classes is strong`() =
        assertEquals(PasswordStrength.STRONG, strength("Abcdefgh12345678"))
}
