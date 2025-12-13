/*
 * Copyright 2024 Smap Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package au.smap.fieldTask.fragments.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import au.smap.fieldTask.activities.SmapMain
import au.smap.fieldTask.preferences.GeneralSharedPreferencesSmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.odk.collect.android.R
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.permissions.PermissionsProvider
import org.odk.collect.android.smap.utilities.LocationRegister
import org.odk.collect.settings.keys.ProjectKeys
import timber.log.Timber
import javax.inject.Inject

/**
 * Smap-specific dialog for requesting location permissions.
 * Shows a dialog asking the user to accept or deny location tracking.
 */
class RequestLocationPermissionsDialogSmap : DialogFragment() {

    @Inject
    lateinit var permissionsProvider: PermissionsProvider

    /*
    We keep this just in case to avoid problems if someone tries to show a dialog after
    the activity's state have been saved. Basically it shouldn't take place since we should control
    the activity state if we want to show a dialog (especially after long tasks).
     */
    override fun show(manager: FragmentManager, tag: String?) {
        try {
            manager
                .beginTransaction()
                .add(this, tag)
                .commit()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DaggerUtils.getComponent(context).inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false

        val currentActivity = requireActivity() as SmapMain
        val locationRegister = LocationRegister()
        val preferences = GeneralSharedPreferencesSmap.getInstance()

        return MaterialAlertDialogBuilder(currentActivity)
            .setTitle(org.odk.collect.strings.R.string.location_runtime_permissions_denied_title)
            .setMessage(locationRegister.messageId)
            .setPositiveButton(R.string.smap_accept2) { _, _ ->
                locationRegister.locationStart(currentActivity, permissionsProvider)
                preferences.save(ProjectKeys.KEY_SMAP_REQUEST_LOCATION_DONE, "accept")
            }
            .setNegativeButton(R.string.smap_deny) { _, _ ->
                preferences.save(ProjectKeys.KEY_SMAP_REQUEST_LOCATION_DONE, "denied")
            }
            .create()
    }

    companion object {
        const val TAG = "LOCATION_PERMISSIONS_DIALOG"
    }
}
