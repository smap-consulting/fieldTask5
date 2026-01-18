package au.smap.fieldTask.activities

import android.os.Bundle
import org.odk.collect.android.R
import org.odk.collect.android.application.CollectComposeThemeProvider
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.androidshared.utils.AppBarUtils.setupAppBarLayout
import org.odk.collect.permissions.PermissionListener
import org.odk.collect.permissions.PermissionsProvider
import org.odk.collect.strings.localization.LocalizedActivity
import javax.inject.Inject

/**
 * smap - Activity for scanning login QR codes
 * Returns server_url, username, and auth_token to SmapLoginActivity
 */
class SmapLoginQRActivity : LocalizedActivity(), CollectComposeThemeProvider {

    @Inject
    lateinit var permissionsProvider: PermissionsProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerUtils.getComponent(this).inject(this)
        setContentView(R.layout.activity_smap_login_qr)
        setupAppBarLayout(this, getString(org.odk.collect.strings.R.string.scan_qr_code_fragment_title))

        permissionsProvider.requestCameraPermission(
            this,
            object : PermissionListener {
                override fun granted() {
                    showScannerFragment()
                }

                override fun additionalExplanationClosed() {
                    finish()
                }
            }
        )
    }

    private fun showScannerFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SmapLoginQRScannerFragment())
            .commit()
    }
}
