package au.smap.fieldTask.aws

import android.content.Context
import au.smap.fieldTask.aws.credentials.CognitoCredentialsProvider
import au.smap.fieldTask.aws.dynamodb.DeviceRepository
import au.smap.fieldTask.aws.services.DeviceRegistrationService
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Dagger module providing AWS-related dependencies.
 *
 * smap - Provides Cognito credentials and DynamoDB repository for device registration.
 */
@Module
class AWSModule {

    /**
     * Provide CognitoCredentialsProvider singleton.
     * Manages AWS credentials via Cognito Identity Pool.
     */
    @Provides
    @Singleton
    fun provideCognitoCredentialsProvider(context: Context): CognitoCredentialsProvider {
        return CognitoCredentialsProvider(context)
    }

    /**
     * Provide DeviceRepository singleton.
     * Repository for DynamoDB device registration operations.
     */
    @Provides
    @Singleton
    fun provideDeviceRepository(credentialsProvider: CognitoCredentialsProvider): DeviceRepository {
        return DeviceRepository(credentialsProvider)
    }

    /**
     * Provide DeviceRegistrationService singleton.
     * High-level service for device registration operations.
     */
    @Provides
    @Singleton
    fun provideDeviceRegistrationService(deviceRepository: DeviceRepository): DeviceRegistrationService {
        return DeviceRegistrationService(deviceRepository)
    }
}
