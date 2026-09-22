package org.odk.collect.android.application.initialization

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.MapView
import org.odk.collect.android.application.MapboxClassInstanceCreator
import org.odk.collect.android.geo.MapConfiguratorProvider
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.ProjectKeys
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
            // smap - creating a MapView is what makes the Maps SDK read the renderer
            // preference, so the order relative to initializeFrameworks matters
            Log.i(MAPS_LOG_TAG, "Creating the warm-up MapView")
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
            // smap - android.util.Log, not Timber: in a release build Timber goes to
            // CrashReportingTree and on to NoopAnalytics, so Timber output is invisible in the
            // field.  This has to be readable with adb logcat on a release build.
            Log.i(MAPS_LOG_TAG, "Requesting the LATEST Google Maps renderer")
            com.google.android.gms.maps.MapsInitializer.initialize(
                context,
                // smap - was pinned to LEGACY in May 2026 (74cb30e990) because the LATEST
                // renderer threw Resources.NotFoundException in the Maps dynamite module on some
                // Android 13 + GMS combinations.  Restored to upstream's LATEST after Google Maps
                // stopped rendering on play-services-maps 20.0.0; the legacy renderer is served by
                // the dynamite module at runtime, so the pin outlived the SDK that supported it.
                // If the Android 13 crash returns, fix it without pinning the renderer.
                com.google.android.gms.maps.MapsInitializer.Renderer.LATEST
            ) { renderer: com.google.android.gms.maps.MapsInitializer.Renderer ->
                Log.i(MAPS_LOG_TAG, "Google Maps renderer in use: $renderer")
            }
            Log.i(MAPS_LOG_TAG, "MapsInitializer.initialize returned without throwing")
        } catch (e: Exception) {
            // smap - this used to be swallowed in silence, which hid why the renderer
            // preference was never registered on some devices
            Log.e(MAPS_LOG_TAG, "MapsInitializer.initialize threw", e)
        } catch (e: Error) {
            Log.e(MAPS_LOG_TAG, "MapsInitializer.initialize failed with an Error", e)
        }
    }

    companion object {
        private var UI_COMPONENTS_INITIALIZED = false

        /** smap - grep for this with adb logcat -s SmapMapsInit */
        private const val MAPS_LOG_TAG = "SmapMapsInit"
    }
}
