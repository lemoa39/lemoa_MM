package com.lemoa.mm.data

import androidx.room.*

@Dao
interface VoucherDao {
    @Query("SELECT COUNT(*) FROM vouchers")
    fun count(): Int
    @Query("SELECT * FROM vouchers ORDER BY id ASC")
    fun getAll(): List<Voucher>
    @Insert
    fun insertAll(v: List<Voucher>)
    @Query("SELECT * FROM vouchers WHERE amount=:amount ORDER BY id ASC LIMIT 1")
    fun findOldestByAmount(amount: Long): Voucher?
    @Query("DELETE FROM vouchers WHERE id=:id")
    fun deleteById(id: Long)
    @Query("DELETE FROM vouchers")
    fun clear()
}

@Dao
interface LogDao {
    @Query("SELECT COUNT(*) FROM logs")
    fun count(): Int
    @Query("SELECT * FROM logs ORDER BY id DESC")
    fun getAllDesc(): List<LogEntry>
    @Query("SELECT * FROM logs ORDER BY id DESC LIMIT 1")
    fun latest(): LogEntry?
    @Insert
    fun insert(l: LogEntry)
    @Query("DELETE FROM logs")
    fun clearAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id=1")
    fun get(): SettingsEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(s: SettingsEntity)
}
