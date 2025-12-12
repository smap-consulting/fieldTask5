/*
 * Copyright (C) 2017 Shobhit
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package au.smap.fieldTask.preferences

import android.content.Context
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.preferences.Defaults
import org.odk.collect.settings.keys.ProtectedProjectKeys
import org.odk.collect.shared.settings.Settings

/**
 * Smap wrapper for admin (protected) settings in fieldTask5.
 * Provides backward-compatible API for AdminSharedPreferences while using
 * the new Settings abstraction.
 */
class AdminSharedPreferencesSmap(private val settings: Settings) {

    constructor(context: Context) : this(
        DaggerUtils.getComponent(context).settingsProvider().getProtectedSettings()
    )

    /**
     * Get a preference value. Admin preferences are booleans except for the password.
     */
    fun get(key: String): Any {
        return if (key == ProtectedProjectKeys.KEY_ADMIN_PW) {
            settings.getString(key) ?: getDefault(key)
        } else {
            settings.getBoolean(key)
        }
    }

    /**
     * Get the default value for a key.
     */
    fun getDefault(key: String): Any {
        return if (key == ProtectedProjectKeys.KEY_ADMIN_PW) {
            ""
        } else {
            true
        }
    }

    /**
     * Reset a preference to its default value.
     */
    fun reset(key: String) {
        val defaultValue = getDefault(key)
        save(key, defaultValue)
    }

    /**
     * Save a preference value.
     */
    fun save(key: String, value: Any?) {
        settings.save(key, value)
    }

    /**
     * Clear all admin preferences.
     */
    fun clear() {
        for (key in ProtectedProjectKeys.allKeys()) {
            reset(key)
        }
    }

    /**
     * Get all admin preferences.
     */
    fun getAll(): Map<String, *> {
        return settings.getAll()
    }

    /**
     * Get the underlying Settings instance.
     */
    fun getSettings(): Settings {
        return settings
    }

    companion object {
        /**
         * Get singleton instance using dependency injection.
         * Uses Collect.getInstance() to get the application context.
         *
         * @deprecated Use constructor injection or get Settings from SettingsProvider instead
         */
        @JvmStatic
        @Deprecated("Use constructor injection instead")
        fun getInstance(): AdminSharedPreferencesSmap {
            val context = org.odk.collect.android.application.Collect.getInstance()
            val settingsProvider = DaggerUtils.getComponent(context).settingsProvider()
            return AdminSharedPreferencesSmap(settingsProvider.getProtectedSettings())
        }
    }
}
