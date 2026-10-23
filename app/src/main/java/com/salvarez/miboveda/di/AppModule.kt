package com.salvarez.miboveda.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.salvarez.miboveda.data.crypto.Argon2KeyDerivation
import com.salvarez.miboveda.data.crypto.KeyDerivation
import com.salvarez.miboveda.data.repository.VaultRepositoryImpl
import com.salvarez.miboveda.domain.repository.VaultRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindKeyDerivation(impl: Argon2KeyDerivation): KeyDerivation

    @Binds
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    companion object {
        @Provides
        @Singleton
        fun provideVaultPreferences(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("vault_prefs") }
    }
}
