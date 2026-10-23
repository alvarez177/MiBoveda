package com.salvarez.miboveda.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val autoLockTimeout: Flow<AutoLockTimeout> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            AutoLockTimeout.entries.firstOrNull { it.name == prefs[AUTO_LOCK_TIMEOUT] } ?: AutoLockTimeout.DEFAULT
        }

    override suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        dataStore.edit { it[AUTO_LOCK_TIMEOUT] = timeout.name }
    }

    private companion object {
        val AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout")
    }
}
