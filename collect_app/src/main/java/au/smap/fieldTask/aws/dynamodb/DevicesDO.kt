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
    var userIdent: String = "",

    /**
     * When this registration was last written, milliseconds since the epoch.
     *
     * The rows carried nothing to date them, so a registration from years ago looked exactly
     * like one from today and dead ones could not be told from live.  Utilities re-asserts a
     * registration weekly, so a live device restamps itself within a week.
     *
     * Absent on rows written by fieldTask 4 and by versions before this, so absence means
     * unknown, not dead.
     */
    @get:DynamoDBAttribute(attributeName = "registeredTime")
    var registeredTime: Long = 0
) {
    companion object {
        /**
         * DynamoDB table name - same as fieldTask4 for backward compatibility
         */
        const val TABLE_NAME = AWSConfiguration.DYNAMODB_DEVICES_TABLE
    }
}
