package dev.skelos.spotifyadmute

import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var statusCard: MaterialCardView
    private lateinit var statusEmoji: TextView
    private lateinit var statusTitle: TextView
    private lateinit var statusBody: TextView
    private lateinit var primaryButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusCard = findViewById(R.id.statusCard)
        statusEmoji = findViewById(R.id.statusEmoji)
        statusTitle = findViewById(R.id.statusTitle)
        statusBody = findViewById(R.id.statusBody)
        primaryButton = findViewById(R.id.primaryButton)
        primaryButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        render(isNotificationListenerEnabled())
    }

    private fun render(active: Boolean) {
        if (active) {
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_active_bg))
            val fg = ContextCompat.getColor(this, R.color.status_active_fg)
            statusEmoji.text = "✓"
            statusEmoji.setTextColor(fg)
            statusTitle.setTextColor(fg)
            statusBody.setTextColor(fg)
            statusTitle.setText(R.string.status_active_title)
            statusBody.setText(R.string.status_active_body)
            primaryButton.setText(R.string.btn_manage)
            primaryButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.status_active_bg)
            )
            primaryButton.setTextColor(fg)
        } else {
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_inactive_bg))
            val fg = ContextCompat.getColor(this, R.color.status_inactive_fg)
            statusEmoji.text = "!"
            statusEmoji.setTextColor(fg)
            statusTitle.setTextColor(fg)
            statusBody.setTextColor(fg)
            statusTitle.setText(R.string.status_inactive_title)
            statusBody.setText(R.string.status_inactive_body)
            primaryButton.setText(R.string.btn_grant)
            primaryButton.backgroundTintList = null
            primaryButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?: return false
        val cn = ComponentName(this, SpotifyAdMuteListener::class.java).flattenToString()
        return flat.split(":").any { it.equals(cn, ignoreCase = true) }
    }
}
