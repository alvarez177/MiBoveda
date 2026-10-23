package com.salvarez.miboveda.di

import android.content.Context
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.salvarez.miboveda.data.crypto.Argon2KeyDerivation
import com.salvarez.miboveda.data.crypto.KeyDerivation
import com.salvarez.miboveda.data.repository.EntryRepositoryImpl
import com.salvarez.miboveda.data.repository.SettingsRepositoryImpl
import com.salvarez.miboveda.data.repository.VaultRepositoryImpl
import com.salvarez.miboveda.domain.repository.SettingsRepository
import com.salvarez.miboveda.domain.usecase.LockVault
import com.salvarez.miboveda.domain.usecase.ObserveAutoLockTimeout
import com.salvarez.miboveda.presentation.lock.AutoLockController
import com.salvarez.miboveda.domain.repository.EntryRepository
import com.salvarez.miboveda.domain.repository.VaultRepository
import com.salvarez.miboveda.presentation.common.AndroidSensitiveClipboard
import com.salvarez.miboveda.presentation.common.SensitiveClipboard
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindKeyDerivation(impl: Argon2KeyDerivation): KeyDerivation

    @Binds
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    @Binds
    abstract fun bindEntryRepository(impl: EntryRepositoryImpl): EntryRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        @Provides
        @Singleton
        fun provideAutoLockController(
            vaultRepository: VaultRepository,
            lockVault: LockVault,
            observeTimeout: ObserveAutoLockTimeout,
            clipboard: SensitiveClipboard,
        ) = AutoLockController(
            vaultRepository = vaultRepository,
            lockVault = lockVault,
            observeTimeout = observeTimeout,
            clipboard = clipboard,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            elapsedRealtimeMillis = SystemClock::elapsedRealtime,
        )

        @Provides
        @Singleton
        fun provideSensitiveClipboard(@ApplicationContext context: Context): SensitiveClipboard =
            AndroidSensitiveClipboard(context, CoroutineScope(SupervisorJob() + Dispatchers.Main))

        @Provides
        @Singleton
        fun provideVaultPreferences(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("vault_prefs") }
    }
}
