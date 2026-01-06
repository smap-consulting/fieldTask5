package au.smap.fieldTask.aws.config

import com.amazonaws.regions.Regions
import org.odk.collect.android.BuildConfig

/**
 * AWS configuration for Cognito and DynamoDB.
 * Reads values from BuildConfig which are populated from secrets.properties.
 *
 * smap - This replaces the fieldTask4 AWSConfiguration.java
 */
object AWSConfiguration {

    /**
     * AWS Cognito Identity Pool region
     */
    val COGNITO_REGION: Regions
        get() = Regions.fromName(BuildConfig.AMAZON_COGNITO_REGION)

    /**
     * AWS Cognito Identity Pool ID for unauthenticated (guest) access
     */
    val COGNITO_IDENTITY_POOL_ID: String
        get() = BuildConfig.AMAZON_COGNITO_IDENTITY_POOL_ID

    /**
     * AWS DynamoDB region
     */
    val DYNAMODB_REGION: Regions
        get() = Regions.fromName(BuildConfig.AMAZON_DYNAMODB_REGION)

    /**
     * DynamoDB table name for device registration
     * This is the same table used by fieldTask4 for backward compatibility
     */
    const val DYNAMODB_DEVICES_TABLE = "fieldtask-mobilehub-447720176-devices"

    /**
     * Validate that required configuration is present
     * @throws IllegalStateException if configuration is invalid
     */
    fun validate() {
        require(COGNITO_IDENTITY_POOL_ID.isNotBlank()) {
            "AMAZON_COGNITO_IDENTITY_POOL_ID must be set in secrets.properties"
        }
        require(BuildConfig.AMAZON_COGNITO_REGION.isNotBlank()) {
            "AMAZON_COGNITO_REGION must be set in secrets.properties"
        }
        require(BuildConfig.AMAZON_DYNAMODB_REGION.isNotBlank()) {
            "AMAZON_DYNAMODB_REGION must be set in secrets.properties"
        }
    }
}
