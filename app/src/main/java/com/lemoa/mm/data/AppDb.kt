package com.lemoa.mm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Voucher::class, LogEntry::class, SettingsEntity::class], version = 1)
abstract class AppDb : RoomDatabase() {
    abstract fun voucherDao(): VoucherDao
    abstract fun logDao(): LogDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var INSTANCE: AppDb? = null
        fun get(context: Context): AppDb = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "lemoa_mm.db").build().also { INSTANCE = it }
        }
    }
}
