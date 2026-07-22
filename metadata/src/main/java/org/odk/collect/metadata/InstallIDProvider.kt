package org.odk.collect.metadata

import org.odk.collect.shared.settings.Settings
import org.odk.collect.shared.strings.RandomString

interface InstallIDProvider {
    val installID: String
}

class SettingsInstallIDProvider(
    private val metaPreferences: Settings,
    private val preferencesKey: String,
    private val prefix: String = "collect" // smap: allow flavor specific prefix
) : InstallIDProvider {

    override val installID: String
        get() {
            return if (metaPreferences.contains(preferencesKey)) {
                metaPreferences.getString(preferencesKey) ?: generateAndStoreInstallID()
            } else {
                generateAndStoreInstallID()
            }
        }

    private fun generateAndStoreInstallID(): String {
        val installID = "$prefix:" + RandomString.randomString(16)
        metaPreferences.save(preferencesKey, installID)
        return installID
    }
}
