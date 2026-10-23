package com.salvarez.miboveda.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.salvarez.miboveda.domain.model.AutoLockTimeout
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dataStore by lazy { PreferenceDataStoreFactory.create { File(folder.root, "settings.preferences_pb") } }
    private val repository by lazy { SettingsRepositoryImpl(dataStore) }

    @Test
    fun `defaults to locking immediately`() = runTest {
        assertEquals(AutoLockTimeout.IMMEDIATE, repository.autoLockTimeout.first())
    }

    @Test
    fun `stores the chosen timeout`() = runTest {
        repository.setAutoLockTimeout(AutoLockTimeout.FIVE_MINUTES)
        assertEquals(AutoLockTimeout.FIVE_MINUTES, repository.autoLockTimeout.first())
    }

    @Test
    fun `an unknown stored value falls back to the default`() = runTest {
        dataStore.edit { it[stringPreferencesKey("auto_lock_timeout")] = "NOT_A_TIMEOUT" }
        assertEquals(AutoLockTimeout.DEFAULT, repository.autoLockTimeout.first())
    }
}
