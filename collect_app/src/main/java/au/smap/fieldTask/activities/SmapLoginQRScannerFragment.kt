package au.smap.fieldTask.activities

import android.app.Activity
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.odk.collect.android.fragments.BarCodeScannerFragment
import org.odk.collect.androidshared.ui.ToastUtils.showShortToast
import timber.log.Timber

/**
 * smap - QR scanner fragment for login credentials
 * Parses QR codes containing server_url, username, and auth_token
 */
class SmapLoginQRScannerFragment : BarCodeScannerFragment() {

    private val gson = Gson()

    override fun handleScanningResult(result: String) {
        Timber.i("Smap login QR scan result received")

        try {
            // Parse the QR code JSON as an object with direct key-value pairs
            val jsonObject = gson.fromJson(result, JsonObject::class.java)

            // Extract the login credentials
            val serverUrl = jsonObject.get("server_url")?.asString
            val username = jsonObject.get("username")?.asString
            val authToken = jsonObject.get("auth_token")?.asString

            // Return the result to the calling activity
            val resultIntent = Intent()
            resultIntent.putExtra("server_url", serverUrl)
            resultIntent.putExtra("username", username)
            resultIntent.putExtra("auth_token", authToken)

            requireActivity().setResult(Activity.RESULT_OK, resultIntent)
            requireActivity().finish()

        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Smap login QR code")
            showShortToast(getString(org.odk.collect.strings.R.string.invalid_qrcode))
            restartScanning()
        }
    }

    override fun isQrOnly(): Boolean {
        return true
    }
}
