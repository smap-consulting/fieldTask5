package org.odk.collect.android.application

import androidx.fragment.app.Fragment
import org.odk.collect.maps.MapConfigurator
import org.odk.collect.maps.MapFragment
import timber.log.Timber

object MapboxClassInstanceCreator {

    private const val MAP_FRAGMENT = "org.odk.collect.mapbox.MapboxMapFragment"

    @JvmStatic
    fun isMapboxAvailable(): Boolean {
        return try {
            Class.forName(MAP_FRAGMENT)
            System.loadLibrary("mapbox-common")
            // The build omits the Mapbox libraries for architectures it does not support, so this
            // has to check the one that actually creates the map.  Checking only mapbox-common
            // lets the app offer Mapbox and then die in Map.initialize, which lives in here.
            System.loadLibrary("mapbox-maps")
            true
        } catch (e: Throwable) {
            Timber.i(e, "Mapbox is not available on this device")
            false
        }
    }

    fun createMapboxMapFragment(configuration: String): MapFragment {
        return Class.forName(MAP_FRAGMENT)
            .getConstructor(String::class.java)
            .newInstance(configuration) as MapFragment
    }

    @JvmStatic
    fun createMapBoxInitializationFragment(): Fragment {
        return Class.forName("org.odk.collect.mapbox.MapBoxInitializationFragment")
            .getConstructor()
            .newInstance() as Fragment
    }

    @JvmStatic
    fun createMapboxMapConfigurator(configuration: String): MapConfigurator {
        return Class.forName("org.odk.collect.mapbox.MapboxMapConfigurator")
            .getConstructor(String::class.java)
            .newInstance(configuration) as MapConfigurator
    }

}
