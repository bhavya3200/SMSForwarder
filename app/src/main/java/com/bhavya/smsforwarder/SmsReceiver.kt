\
package com.bhavya.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import java.util.concurrent.Executors
import okhttp3.*
import org.json.JSONObject

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) return

        val prefs = context.getSharedPreferences("cfg", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        if (!enabled) return

        val viaSms = prefs.getBoolean("viaSms", true)
        val viaTelegram = prefs.getBoolean("viaTelegram", false)
        val viaWa = prefs.getBoolean("viaWa", false)

        val target = prefs.getString("target", "")?.trim().orEmpty()
        val botToken = prefs.getString("botToken", "")?.trim().orEmpty()
        val chatId = prefs.getString("chatId", "")?.trim().orEmpty()

        val waPhoneNumberId = prefs.getString("waPhoneNumberId", "")?.trim().orEmpty()
        val waToken = prefs.getString("waToken", "")?.trim().orEmpty()
        val waTo = prefs.getString("waTo", "")?.trim().orEmpty()

        val skipOtp = prefs.getBoolean("skipOtp", true)

        val whitelist = parseList(prefs.getString("whitelistSenders", ""))
        val blacklist = parseList(prefs.getString("blacklistSenders", ""))
        val onlyIfKw  = parseList(prefs.getString("onlyKeywords", ""))
        val neverKw   = parseList(prefs.getString("neverKeywords", ""))

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (msgs.isEmpty()) return

        val from = msgs.first().originatingAddress ?: "Unknown"
        val body = msgs.joinToString(separator = "") { it.displayMessageBody ?: "" }

        if (body.startsWith("[FWD]")) return
        if (matchesSender(from, blacklist)) return
        if (whitelist.isNotEmpty() && !matchesSender(from, whitelist)) return
        if (matchesKeyword(body, neverKw)) return
        if (onlyIfKw.isNotEmpty() && !matchesKeyword(body, onlyIfKw)) return
        if (skipOtp && looksLikeOtp(body)) return

        val text = "[FWD] From: $from\n$body"

        val pending = goAsync()
        Executors.newSingleThreadExecutor().execute {
            try {
                if (viaSms && target.isNotEmpty()) sendViaSms(context, target, text)
                if (viaTelegram && botToken.isNotEmpty() && chatId.isNotEmpty()) sendToTelegram(botToken, chatId, text)
                if (viaWa && waPhoneNumberId.isNotEmpty() && waToken.isNotEmpty() && waTo.isNotEmpty()) sendToWhatsApp(waPhoneNumberId, waToken, waTo, text)
            } finally { pending.finish() }
        }
    }

    private fun parseList(raw: String?): List<String> =
        raw?.split('\n', ',', ';')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    private fun normalizeNumber(s: String): String {
        val digits = s.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    private fun matchesSender(senderRaw: String, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) return false
        val sender = senderRaw.trim()
        val senderDigits = normalizeNumber(sender)
        val up = sender.uppercase()
        for (p in patterns) {
            val isRegex = p.startsWith("re:", ignoreCase = true)
            val pat = if (isRegex) p.substring(3) else p
            if (isRegex) {
                runCatching { if (Regex(pat, RegexOption.IGNORE_CASE).containsMatchIn(sender)) return true }.getOrNull()
            } else {
                val pDigits = normalizeNumber(p)
                if (pDigits.isNotEmpty() && senderDigits.isNotEmpty()) {
                    if (senderDigits == pDigits || senderDigits.endsWith(pDigits)) return true
                }
                if (up.contains(p.uppercase())) return true
            }
        }
        return false
    }

    private fun matchesKeyword(textRaw: String, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) return false
        val text = textRaw
        val up = text.uppercase()
        for (p in patterns) {
            val isRegex = p.startsWith("re:", ignoreCase = true)
            val pat = if (isRegex) p.substring(3) else p
            if (isRegex) {
                runCatching { if (Regex(pat, RegexOption.IGNORE_CASE).containsMatchIn(text)) return true }.getOrNull()
            } else {
                if (up.contains(p.uppercase())) return true
            }
        }
        return false
    }

    private fun looksLikeOtp(s: String): Boolean {
        val up = s.uppercase()
        if (!up.contains("OTP") && !up.contains("ONE TIME") && !up.contains("VERIFY")) return false
        val hasCode = Regex("""\b\d{4,8}\b""").find(s) != null
        return hasCode
    }

    private fun sendViaSms(context: Context, target: String, text: String) {
        try {
            val sms: SmsManager =
                context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            val parts = sms.divideMessage(text)
            sms.sendMultipartTextMessage(target, null, parts, null, null)
        } catch (_: Exception) { }
    }

    private fun sendToTelegram(token: String, chatId: String, text: String) {
        val url = "https://api.telegram.org/bot$token/sendMessage"
        val form = FormBody.Builder().add("chat_id", chatId).add("text", text).build()
        val req = Request.Builder().url(url).post(form).build()
        OkHttpClient().newCall(req).execute().use { }
    }

    private fun sendToWhatsApp(phoneNumberId: String, token: String, to: String, text: String) {
        val url = "https://graph.facebook.com/v20.0/$phoneNumberId/messages"
        val payload = JSONObject()
            .put("messaging_product", "whatsapp")
            .put("to", to)
            .put("type", "text")
            .put("text", JSONObject().put("body", text))
            .toString()
        val body = RequestBody.create(MediaType.parse("application/json"), payload)
        val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").post(body).build()
        OkHttpClient().newCall(req).execute().use { }
    }
}
