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
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.shared.settings.Settings
import timber.log.Timber

/**
 * Smap wrapper for the new Settings system in fieldTask5.
 * Provides backward-compatible API for GeneralSharedPreferences while using
 * the new Settings abstraction.
 */
class GeneralSharedPreferencesSmap(private val settings: Settings) {

    constructor(context: Context) : this(
        DaggerUtils.getComponent(context).settingsProvider().getUnprotectedSettings()
    )

    /**
     * Get a preference value with automatic type detection based on defaults.
     * This maintains backward compatibility with the old GeneralSharedPreferences API.
     *
     * @deprecated Use type-specific methods (getString, getBoolean, etc.) instead
     */
    @Deprecated("Use type-specific getters instead")
    fun get(key: String): Any? {
        val defaultValue: Any? = try {
            Defaults.unprotected[key]
        } catch (e: Exception) {
            Timber.e("Default for %s not found", key)
            null
        }

        return when (defaultValue) {
            null, is String -> settings.getString(key)
            is Boolean -> settings.getBoolean(key)
            is Long -> settings.getLong(key)
            is Int -> settings.getInt(key)
            is Float -> settings.getFloat(key)
            else -> settings.getString(key)
        }
    }

    /**
     * Reset a preference to its default value.
     */
    fun reset(key: String) {
        settings.reset(key)
    }

    /**
     * Save a preference value with automatic type detection.
     *
     * @return this instance for method chaining
     */
    fun save(key: String, value: Any?): GeneralSharedPreferencesSmap {
        settings.save(key, value)
        return this
    }

    /**
     * Get a boolean preference value.
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            settings.getBoolean(key)
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Get a string set preference value.
     */
    fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>? {
        return settings.getStringSet(key) ?: defaultValue
    }

    /**
     * Clear all preferences and reset to defaults.
     */
    fun clear() {
        for (entry in getAll().entries) {
            reset(entry.key)
        }
    }

    /**
     * Get all preferences.
     */
    fun getAll(): Map<String, *> {
        return settings.getAll()
    }

    /**
     * Load default preferences (clear and reload).
     */
    fun loadDefaultPreferences() {
        clear()
        reloadPreferences()
    }

    /**
     * Reload preferences from defaults.
     */
    fun reloadPreferences() {
        for ((key, _) in Defaults.unprotected) {
            save(key, get(key))
        }
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
        fun getInstance(): GeneralSharedPreferencesSmap {
            val context = org.odk.collect.android.application.Collect.getInstance()
            val settingsProvider = DaggerUtils.getComponent(context).settingsProvider()
            return GeneralSharedPreferencesSmap(settingsProvider.getUnprotectedSettings())
        }

        /**
         * Check if auto-send is enabled.
         */
        @JvmStatic
        fun isAutoSendEnabled(): Boolean {
            val instance = getInstance()
            return instance.get(org.odk.collect.settings.keys.ProjectKeys.KEY_AUTOSEND) != "off"
        }
    }
}
