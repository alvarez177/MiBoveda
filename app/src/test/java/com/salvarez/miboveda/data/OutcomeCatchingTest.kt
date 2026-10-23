package com.salvarez.miboveda.data

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Test

class OutcomeCatchingTest {
    @Test
    fun `returns success with the block result`() {
        assertEquals(Outcome.Success(42), outcomeOf { 42 })
    }

    @Test
    fun `maps exceptions to storage failure by default`() {
        val result = outcomeOf { throw IOException("disk") }
        assertEquals(Outcome.Failure(VaultError.StorageFailure), result)
    }

    @Test
    fun `uses the custom error mapper`() {
        val result = outcomeOf(mapError = { VaultError.WrongMasterPassword }) {
            throw IllegalStateException()
        }
        assertEquals(Outcome.Failure(VaultError.WrongMasterPassword), result)
    }

    @Test(expected = CancellationException::class)
    fun `rethrows cancellation`() {
        outcomeOf { throw CancellationException("cancelled") }
    }
}
