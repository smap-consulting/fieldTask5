package org.odk.collect.android.application.initialization

import android.content.Context

import org.odk.collect.android.geo.MapConfiguratorProvider
import org.odk.collect.osmdroid.OsmDroidInitializer
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.utilities.UserAgentProvider
import timber.log.Timber
import javax.inject.Inject

class MapsInitializer @Inject constructor(
    private val context: Context,
    private val settingsProvider: SettingsProvider,
    private val userAgentProvider: UserAgentProvider
) {

    fun initialize() {
        resetToAvailableFramework()

        if (!FRAMEWORKS_INITIALIZED) {
            initializeFrameworks()
        }
    }

    private fun resetToAvailableFramework() {
        MapConfiguratorProvider.initOptions(context)
        val availableBaseMaps = MapConfiguratorProvider.getIds()
        val baseMapSetting =
            settingsProvider.getUnprotectedSettings().getString(ProjectKeys.KEY_BASEMAP_SOURCE)
        if (!availableBaseMaps.contains(baseMapSetting)) {
            settingsProvider.getUnprotectedSettings().save(
                ProjectKeys.KEY_BASEMAP_SOURCE,
                availableBaseMaps[0]
            )
        }
    }

    private fun initializeFrameworks() {
        try {
            com.google.android.gms.maps.MapsInitializer.initialize(
                context,
                com.google.android.gms.maps.MapsInitializer.Renderer.LATEST
            ) { renderer: com.google.android.gms.maps.MapsInitializer.Renderer ->
                when (renderer) {
                    com.google.android.gms.maps.MapsInitializer.Renderer.LATEST -> Timber.d("The latest version of Google Maps renderer is used.")
                    com.google.android.gms.maps.MapsInitializer.Renderer.LEGACY -> Timber.d("The legacy version of Google Maps renderer is used.")
                }
            }
            // smap - removed MapView(context).onCreate(null) here: caused ANR via synchronous
            // smap - Binder IPC on main thread. MapsInitializer.initialize() above is sufficient.
            OsmDroidInitializer.initialize(userAgentProvider.userAgent)
            FRAMEWORKS_INITIALIZED = true // smap - was never set to true in upstream
        } catch (ignore: Exception) {
            // ignored
        } catch (ignore: Error) {
            // ignored
        }
    }

    companion object {
        private var FRAMEWORKS_INITIALIZED = false
    }
}
