package au.smap.fieldTask.aws.dynamodb

import au.smap.fieldTask.aws.config.AWSConfiguration
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

/**
 * DynamoDB data object for device registration.
 * Maps to table: fieldtask-mobilehub-447720176-devices
 *
 * smap - Migrated from fieldTask4 with AWS SDK v3 annotations.
 * CRITICAL: Attribute names MUST match fieldTask4 for backward compatibility.
 * Both fieldTask4 and fieldTask5 devices share the same DynamoDB table.
 */
@DynamoDbBean
data class DevicesDO(
    /**
     * Firebase Cloud Messaging registration token (partition key)
     */
    @get:DynamoDbPartitionKey
    @get:DynamoDbAttribute("registrationId")
    var registrationId: String = "",

    /**
     * Smap server URL
     */
    @get:DynamoDbAttribute("smapServer")
    var smapServer: String = "",

    /**
     * User identifier (username)
     */
    @get:DynamoDbAttribute("userIdent")
    var userIdent: String = ""
) {
    companion object {
        /**
         * DynamoDB table name - same as fieldTask4 for backward compatibility
         */
        const val TABLE_NAME = AWSConfiguration.DYNAMODB_DEVICES_TABLE
    }
}
