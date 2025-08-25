package com.lemoa.mm.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.telephony.SmsManager
import com.lemoa.mm.data.AppDb
import com.lemoa.mm.data.LogEntry
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class IncomingSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle: Bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")

        val messages = pdus.mapNotNull { pdu ->
            try {
                if (format != null) {
                    SmsMessage.createFromPdu(pdu as ByteArray, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }
            } catch (_: Exception) { null }
        }
        if (messages.isEmpty()) return

        val msgBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val from = messages.firstOrNull()?.originatingAddress ?: return

        GlobalScope.launch(Dispatchers.IO) {
            val db = AppDb.get(context)
            val settings = db.settingsDao().get() ?: com.lemoa.mm.data.SettingsEntity.default()
            val regexes = listOf(settings.regexMtn, settings.regexAirtel)

            var amount: Long? = null
            var number: String? = null
            for (rx in regexes) {
                try {
                    val m = Pattern.compile(rx, Pattern.DOTALL).matcher(msgBody)
                    if (m.find() && m.groupCount() >= 2) {
                        val amtStr = m.group(1).replace("[^0-9]".toRegex(), "")
                        amount = amtStr.toLongOrNull()
                        number = m.group(2)
                        break
                    }
                } catch (_: Exception) {}
            }
            if (amount == null) return@launch
            if (number.isNullOrBlank()) number = from

            val voucher = db.voucherDao().findOldestByAmount(amount!!)
            if (voucher != null) {
                val reply = settings.template
                    .replace("{code}", voucher.code)
                    .replace("{amount}", amount.toString())
                    .replace("{number}", number!!)

                SmsManager.getDefault().sendTextMessage(number, null, reply, null, null)

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val now = sdf.format(Date())
                db.logDao().insert(LogEntry(0, number!!, amount!!, voucher.code, now))
                db.voucherDao().deleteById(voucher.id)
            }
        }
    }
}
