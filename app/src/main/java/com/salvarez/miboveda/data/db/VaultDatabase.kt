package com.salvarez.miboveda.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EntryEntity::class], version = 1)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
