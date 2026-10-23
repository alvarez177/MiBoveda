package com.salvarez.miboveda.domain

import com.salvarez.miboveda.domain.error.VaultError
import org.junit.Assert.assertEquals
import org.junit.Test

class OutcomeTest {
    @Test
    fun `map transforms a success`() {
        assertEquals(Outcome.Success(4), Outcome.Success(2).map { it * 2 })
    }

    @Test
    fun `map keeps a failure untouched`() {
        val failure: Outcome<Int> = Outcome.Failure(VaultError.VaultLocked)
        assertEquals(failure, failure.map { it * 2 })
    }
}
