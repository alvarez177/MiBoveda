package com.salvarez.miboveda.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Keeps the database key wrapped by the biometric key. Having it stored means biometric unlock is on. */
class BiometricBlobStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val isEnabled: Flow<Boolean> = dataStore.data.map { it[WRAPPED_DEK] != null }

    suspend fun read(): ByteArray? =
        dataStore.data.first()[WRAPPED_DEK]?.let { Base64.getDecoder().decode(it) }

    suspend fun save(blob: ByteArray) {
        dataStore.edit { it[WRAPPED_DEK] = Base64.getEncoder().encodeToString(blob) }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(WRAPPED_DEK) }
    }

    private companion object {
        val WRAPPED_DEK = stringPreferencesKey("biometric_wrapped_dek")
    }
}
