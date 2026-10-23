package com.salvarez.miboveda.data

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [block] and turns technical exceptions into a domain [VaultError].
 * [CancellationException] is always rethrown so coroutine cancellation keeps working.
 */
inline fun <T> outcomeOf(
    mapError: (Exception) -> VaultError = { VaultError.StorageFailure },
    block: () -> T,
): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Outcome.Failure(mapError(e))
}
