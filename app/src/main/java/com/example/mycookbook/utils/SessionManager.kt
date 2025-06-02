package com.example.mycookbook.utils

import android.content.Context
import java.util.UUID
import androidx.preference.PreferenceManager

class SessionManager(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val KEY_GUEST_ID = "guest_id"
    }

    fun getGuestId(): String {
        var guestId = prefs.getString(KEY_GUEST_ID, null)
        if (guestId == null) {
            guestId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_GUEST_ID, guestId).apply()
        }
        return guestId
    }

    fun clearGuestSession() {
        prefs.edit().remove(KEY_GUEST_ID).apply()
    }
}
