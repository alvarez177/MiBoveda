package com.salvarez.miboveda.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.salvarez.miboveda.data.crypto.KdfParams
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class VaultHeaderStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    /** Returns null when no vault has been created yet. */
    suspend fun read(): VaultHeader? {
        val prefs = dataStore.data.first()
        return VaultHeader(
            version = prefs[VERSION] ?: return null,
            kdfParams = KdfParams(
                iterations = prefs[ITERATIONS] ?: return null,
                memoryKib = prefs[MEMORY_KIB] ?: return null,
                parallelism = prefs[PARALLELISM] ?: return null,
            ),
            salt = Base64.getDecoder().decode(prefs[SALT] ?: return null),
            wrappedDek = Base64.getDecoder().decode(prefs[WRAPPED_DEK] ?: return null),
            recoveryWrappedDek = prefs[RECOVERY_WRAPPED_DEK]?.let { Base64.getDecoder().decode(it) },
        )
    }

    suspend fun save(header: VaultHeader) {
        dataStore.edit { prefs ->
            prefs[VERSION] = header.version
            prefs[ITERATIONS] = header.kdfParams.iterations
            prefs[MEMORY_KIB] = header.kdfParams.memoryKib
            prefs[PARALLELISM] = header.kdfParams.parallelism
            prefs[SALT] = Base64.getEncoder().encodeToString(header.salt)
            prefs[WRAPPED_DEK] = Base64.getEncoder().encodeToString(header.wrappedDek)
            val recovery = header.recoveryWrappedDek
            if (recovery != null) prefs[RECOVERY_WRAPPED_DEK] = Base64.getEncoder().encodeToString(recovery)
            else prefs.remove(RECOVERY_WRAPPED_DEK)
        }
    }

    val hasRecoveryKey: Flow<Boolean> = dataStore.data.map { it[RECOVERY_WRAPPED_DEK] != null }

    suspend fun saveRecovery(recoveryWrappedDek: ByteArray) {
        dataStore.edit { it[RECOVERY_WRAPPED_DEK] = Base64.getEncoder().encodeToString(recoveryWrappedDek) }
    }

    private companion object {
        val VERSION = intPreferencesKey("header_version")
        val ITERATIONS = intPreferencesKey("kdf_iterations")
        val MEMORY_KIB = intPreferencesKey("kdf_memory_kib")
        val PARALLELISM = intPreferencesKey("kdf_parallelism")
        val SALT = stringPreferencesKey("kdf_salt")
        val WRAPPED_DEK = stringPreferencesKey("wrapped_dek")
        val RECOVERY_WRAPPED_DEK = stringPreferencesKey("recovery_wrapped_dek")
    }
}
