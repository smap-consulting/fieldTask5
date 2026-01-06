package au.smap.fieldTask.aws.dynamodb

import au.smap.fieldTask.aws.config.AWSConfiguration
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable

/**
 * DynamoDB data object for device registration.
 * Maps to table: fieldtask-mobilehub-447720176-devices
 *
 * smap - Migrated from fieldTask4 with AWS Android SDK v2 annotations.
 * CRITICAL: Attribute names MUST match fieldTask4 for backward compatibility.
 * Both fieldTask4 and fieldTask5 devices share the same DynamoDB table.
 */
@DynamoDBTable(tableName = AWSConfiguration.DYNAMODB_DEVICES_TABLE)
data class DevicesDO(
    /**
     * Firebase Cloud Messaging registration token (partition key)
     */
    @get:DynamoDBHashKey(attributeName = "registrationId")
    var registrationId: String = "",

    /**
     * Smap server URL
     */
    @get:DynamoDBAttribute(attributeName = "smapServer")
    var smapServer: String = "",

    /**
     * User identifier (username)
     */
    @get:DynamoDBAttribute(attributeName = "userIdent")
    var userIdent: String = ""
) {
    companion object {
        /**
         * DynamoDB table name - same as fieldTask4 for backward compatibility
         */
        const val TABLE_NAME = AWSConfiguration.DYNAMODB_DEVICES_TABLE
    }
}
