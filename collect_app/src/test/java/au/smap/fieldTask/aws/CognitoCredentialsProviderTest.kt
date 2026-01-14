package au.smap.fieldTask.aws

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import au.smap.fieldTask.aws.credentials.CognitoCredentialsProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for CognitoCredentialsProvider
 */
@RunWith(RobolectricTestRunner::class)
class CognitoCredentialsProviderTest {

    @Test
    fun testProviderCreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = CognitoCredentialsProvider(context)
        assertNotNull(provider)
    }

    @Test
    fun testGetCredentials() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = CognitoCredentialsProvider(context)
        val credentials = provider.credentialsProvider.credentials
        assertNotNull(credentials)
    }
}
