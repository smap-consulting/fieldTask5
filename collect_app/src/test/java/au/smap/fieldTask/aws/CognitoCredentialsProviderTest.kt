package au.smap.fieldTask.aws

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
        val provider = CognitoCredentialsProvider("us-east-1", "test-pool-id")
        assertNotNull(provider)
    }

    @Test
    fun testGetCredentials() {
        val provider = CognitoCredentialsProvider("us-east-1", "test-pool-id")
        val credentials = provider.getCredentials()
        assertNotNull(credentials)
    }
}
