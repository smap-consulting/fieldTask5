package org.odk.collect.android.mainmenu

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.odk.collect.android.R
import org.odk.collect.permissions.PermissionListener
import org.odk.collect.permissions.PermissionsProvider

class PermissionsDialogFragment(
    private val permissionsProvider: PermissionsProvider,
    private val requestPermissionsViewModel: RequestPermissionsViewModel
) : DialogFragment() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false

        // smap - use app_name in permission text
        val view = layoutInflater.inflate(R.layout.permissions_dialog_layout, null)
        view.findViewById<TextView>(R.id.permission_text).text =
            getString(org.odk.collect.strings.R.string.permission_dialog_text, getString(org.odk.collect.strings.R.string.app_name))

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(org.odk.collect.strings.R.string.permission_dialog_title)
            .setView(view)
            .setPositiveButton(org.odk.collect.strings.R.string.ok) { _, _ ->
                requestPermissionsViewModel.permissionsRequested()
                permissionsProvider.requestPermissions(
                    requireActivity(),
                    object : PermissionListener {
                        override fun granted() {}
                    },
                    *requestPermissionsViewModel.permissions
                )
            }
            .create()
    }
}
