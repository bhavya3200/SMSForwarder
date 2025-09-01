package com.bhavya.smsforwarder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bhavya.smsforwarder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val ok = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        ).all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        b.tvStatus.text = if (ok) "SMS permissions granted." else "SMS permissions missing (needed only if forwarding via SMS)."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)

        b.cbEnabled.isChecked = prefs.getBoolean("enabled", false)
        b.cbViaSms.isChecked = prefs.getBoolean("viaSms", true)
        b.etTarget.setText(prefs.getString("target", ""))

        b.cbViaTelegram.isChecked = prefs.getBoolean("viaTelegram", false)
        b.etBotToken.setText(prefs.getString("botToken", ""))
        b.etChatId.setText(prefs.getString("chatId", ""))

        b.cbViaWa.isChecked = prefs.getBoolean("viaWa", false)
        b.etWaPhoneNumberId.setText(prefs.getString("waPhoneNumberId", ""))
        b.etWaToken.setText(prefs.getString("waToken", ""))
        b.etWaTo.setText(prefs.getString("waTo", ""))

        b.cbSkipOtp.isChecked = prefs.getBoolean("skipOtp", true)

        b.etWhitelist.setText(prefs.getString("whitelistSenders", ""))
        b.etBlacklist.setText(prefs.getString("blacklistSenders", ""))
        b.etOnlyKeywords.setText(prefs.getString("onlyKeywords", ""))
        b.etNeverKeywords.setText(prefs.getString("neverKeywords", ""))

        b.btnSave.setOnClickListener {
            prefs.edit()
                .putBoolean("enabled", b.cbEnabled.isChecked)
                .putBoolean("viaSms", b.cbViaSms.isChecked)
                .putString("target", b.etTarget.text.toString().trim())
                .putBoolean("viaTelegram", b.cbViaTelegram.isChecked)
                .putString("botToken", b.etBotToken.text.toString().trim())
                .putString("chatId", b.etChatId.text.toString().trim())
                .putBoolean("viaWa", b.cbViaWa.isChecked)
                .putString("waPhoneNumberId", b.etWaPhoneNumberId.text.toString().trim())
                .putString("waToken", b.etWaToken.text.toString().trim())
                .putString("waTo", b.etWaTo.text.toString().trim())
                .putBoolean("skipOtp", b.cbSkipOtp.isChecked)
                .putString("whitelistSenders", b.etWhitelist.text.toString())
                .putString("blacklistSenders", b.etBlacklist.text.toString())
                .putString("onlyKeywords", b.etOnlyKeywords.text.toString())
                .putString("neverKeywords", b.etNeverKeywords.text.toString())
                .apply()
            ensurePermissionsIfNeeded()
            b.tvStatus.text = "Saved."
        }

        ensurePermissionsIfNeeded()
    }

    private fun ensurePermissionsIfNeeded() {
        val needSmsPerms = getSharedPreferences("cfg", MODE_PRIVATE).getBoolean("viaSms", true)
        if (!needSmsPerms) return
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.RECEIVE_SMS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.SEND_SMS
        if (needed.isNotEmpty()) requestPerms.launch(needed.toTypedArray())
    }
}
