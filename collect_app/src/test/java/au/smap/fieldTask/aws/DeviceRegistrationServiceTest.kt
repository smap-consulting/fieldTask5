package au.smap.fieldTask.aws

import androidx.test.core.app.ApplicationProvider
import au.smap.fieldTask.aws.dynamodb.DeviceRepository
import au.smap.fieldTask.aws.services.DeviceRegistrationService
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Tests for DeviceRegistrationService
 */
@RunWith(RobolectricTestRunner::class)
class DeviceRegistrationServiceTest {

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testServiceCreation() {
        val service = DeviceRegistrationService(deviceRepository)
        assertNotNull(service)
    }
}
