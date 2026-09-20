package org.odk.collect.android.application.initialization

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.MapView
import org.odk.collect.android.application.MapboxClassInstanceCreator
import org.odk.collect.android.geo.MapConfiguratorProvider
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.ProjectKeys
import timber.log.Timber
import javax.inject.Inject

class MapsInitializer @Inject constructor(
    private val context: Context,
    private val settingsProvider: SettingsProvider
) {

    fun initialize() {
        // smap - This has to stay on the main thread.  Building the map configurators loads the
        // Mapbox native libraries and touches Mapbox classes, and Mapbox binds its schedulers to
        // the thread that does so.  Doing it on a bare Thread with no Looper leaves the SDK
        // unusable afterwards - "scheduler is not available for thread", telemetry fails to
        // start, and a worker later aborts the process on an uncaught std::range_error.
        // It was moved off the main thread to avoid an ANR from the Binder IPC in
        // isGooglePlayServicesAvailable; if that returns, defer this call rather than rethread it.
        resetToAvailableFramework()
        initializeFrameworks()
    }

    fun initializeUIComponents(activity: FragmentActivity, fragmentContainer: Int) {
        if (!UI_COMPONENTS_INITIALIZED) {
            val mapView = MapView(activity.application)
            mapView.onCreate(null)
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    mapView.onDestroy()
                }
            })

            if (MapboxClassInstanceCreator.isMapboxAvailable()) {
                activity.supportFragmentManager
                    .beginTransaction()
                    .add(
                        fragmentContainer,
                        MapboxClassInstanceCreator.createMapBoxInitializationFragment()
                    )
                    .commit()
            }

            UI_COMPONENTS_INITIALIZED = true
        }
    }

    private fun resetToAvailableFramework() {
        MapConfiguratorProvider.initOptions(context)
        val availableBaseMaps = MapConfiguratorProvider.getIds()
        val baseMapSetting =
            settingsProvider.getUnprotectedSettings().getString(ProjectKeys.KEY_BASEMAP_SOURCE)
        if (!availableBaseMaps.contains(baseMapSetting) && availableBaseMaps.isNotEmpty()) {
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
                com.google.android.gms.maps.MapsInitializer.Renderer.LEGACY
            ) { renderer: com.google.android.gms.maps.MapsInitializer.Renderer ->
                when (renderer) {
                    com.google.android.gms.maps.MapsInitializer.Renderer.LATEST -> Timber.d("The latest version of Google Maps renderer is used.")
                    com.google.android.gms.maps.MapsInitializer.Renderer.LEGACY -> Timber.d("The legacy version of Google Maps renderer is used.")
                }
            }
        } catch (ignore: Exception) {
            // ignored
        } catch (ignore: Error) {
            // ignored
        }
    }

    companion object {
        private var UI_COMPONENTS_INITIALIZED = false
    }
}
