package com.lemoa.mm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vouchers")
data class Voucher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val code: String
)

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val amount: Long,
    val code: String,
    val ts: String
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val template: String,
    val regexMtn: String,
    val regexAirtel: String
) {
    companion object {
        fun default() = SettingsEntity(
            id = 1,
            template = "Your code is: {code}",
            regexMtn = ".*UGX (\\d[\\d,]*) from (\\+?\\d{9,12}).*",
            regexAirtel = ".*UGX (\\d[\\d,]*) from (\\+?\\d{9,12}).*"
        )
    }
}
