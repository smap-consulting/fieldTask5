package org.odk.collect.settings.keys

object ProjectKeys {

    // server_preferences.xml
    const val KEY_PROTOCOL = "protocol"

    // odk_server_preferences.xml
    const val KEY_SERVER_URL = "server_url"
    const val KEY_USERNAME = "username"
    const val KEY_PASSWORD = "password"

    // user_interface_preferences.xml
    const val KEY_APP_LANGUAGE = "app_language"
    const val KEY_FONT_SIZE = "font_size"
    const val KEY_NAVIGATION = "navigation"

    // map_preferences.xml
    const val KEY_BASEMAP_SOURCE = "basemap_source"

    // basemap styles
    const val KEY_GOOGLE_MAP_STYLE = "google_map_style"
    const val KEY_MAPBOX_MAP_STYLE = "mapbox_map_style"
    const val KEY_USGS_MAP_STYLE = "usgs_map_style"
    const val KEY_CARTO_MAP_STYLE = "carto_map_style"
    const val KEY_REFERENCE_LAYER = "reference_layer"

    // form_management_preferences.xml
    const val KEY_FORM_UPDATE_MODE = "form_update_mode"
    const val KEY_PERIODIC_FORM_UPDATES_CHECK = "periodic_form_updates_check"
    const val KEY_AUTOMATIC_UPDATE = "automatic_update"
    const val KEY_HIDE_OLD_FORM_VERSIONS = "hide_old_form_versions"
    const val KEY_AUTOSEND = "autosend"
    const val KEY_DELETE_AFTER_SEND = "delete_send"
    const val KEY_CONSTRAINT_BEHAVIOR = "constraint_behavior"
    const val KEY_HIGH_RESOLUTION = "high_resolution"
    const val KEY_IMAGE_SIZE = "image_size"
    const val KEY_GUIDANCE_HINT = "guidance_hint"
    const val KEY_EXTERNAL_APP_RECORDING = "external_app_recording"
    const val KEY_INSTANCE_SYNC = "instance_sync"

    // identity_preferences.xml
    const val KEY_ANALYTICS = "analytics"

    // form_metadata_preferences.xml
    const val KEY_METADATA_USERNAME = "metadata_username"
    const val KEY_METADATA_PHONENUMBER = "metadata_phonenumber"
    const val KEY_METADATA_EMAIL = "metadata_email"
    const val KEY_FORM_METADATA = "form_metadata"
    const val KEY_BACKGROUND_LOCATION = "background_location"
    const val KEY_BACKGROUND_RECORDING = "background_recording"

    // experimental_preferences.xml
    const val KEY_DEBUG_FILTERS = "experimental_debug_filters"
    const val KEY_ZXING_SCANNING = "zxing_scanning"
    const val KEY_ENTITIES_SPEC_V2025_1 = "entities_spec_v2025_1"

    // values
    const val PROTOCOL_SERVER = "odk_default"
    const val PROTOCOL_GOOGLE_SHEETS = "google_sheets"
    const val NAVIGATION_SWIPE = "swipe"
    const val NAVIGATION_BUTTONS = "buttons"
    const val NAVIGATION_BOTH = "swipe_buttons"
    const val CONSTRAINT_BEHAVIOR_ON_SWIPE = "on_swipe"

    // basemap section
    const val CATEGORY_BASEMAP = "category_basemap"

    // basemap source values
    const val BASEMAP_SOURCE_GOOGLE = "google"
    const val BASEMAP_SOURCE_MAPBOX = "mapbox"
    const val BASEMAP_SOURCE_OSM = "osm"
    const val BASEMAP_SOURCE_USGS = "usgs"
    const val BASEMAP_SOURCE_CARTO = "carto"

    // remembered defaults
    const val KEY_SAVED_FORM_SORT_ORDER = "instanceUploaderListSortingOrder"
    const val KEY_BLANK_FORM_SORT_ORDER = "formChooserListSortingOrder"

    // smap preferences
    const val KEY_SAVED_USERS = "saved_users" // Saved user credentials for offline login
    const val KEY_SMAP_USE_TOKEN = "smap_use_token" // Use token for logon
    const val KEY_SMAP_SCAN_TOKEN = "smap_scan_token" // Scan the token
    const val KEY_SMAP_AUTH_TOKEN = "auth_token" // Authentication Token
    const val KEY_SMAP_REVIEW_FINAL = "review_final" // Allow review of Form after finalising
    const val KEY_SMAP_FORCE_TOKEN = "force_token" // Require the use of tokens for authentication
    const val KEY_SMAP_USER_LOCATION = "smap_gps_trail" // Record a user trail
    const val KEY_SMAP_USER_SAVE_LOCATION = "smap_gps_trail" // Backup of decision to record the user trail
    const val KEY_SMAP_LOCATION_TRIGGER = "location_trigger" // Enable triggering of forms by location
    const val KEY_SMAP_ODK_STYLE_MENUS = "odk_style_menus" // Show ODK style menus as well as refresh
    const val KEY_SMAP_ODK_INSTANCENAME = "odk_instancename" // Allow user to change instance name
    const val KEY_SMAP_ODK_MARK_FINALIZED = "odk_mark_finalized" // Allow user to change instance name
    const val KEY_SMAP_PREVENT_DISABLE_TRACK = "disable_prevent_track" // Prevent the user from disabling tracking
    const val KEY_SMAP_ENABLE_GEOFENCE = "enable_geofence" // Monitor location for geofence
    const val KEY_SMAP_ODK_ADMIN_MENU = "odk_admin_menu" // Show ODK admin menu
    const val KEY_SMAP_ADMIN_SERVER_MENU = "admin_server_menu" // Show server menu in general settings
    const val KEY_SMAP_ADMIN_META_MENU = "admin_meta_menu" // Show meta menu in general settings
    const val KEY_SMAP_EXIT_TRACK_MENU = "smap_exit_track_menu" // Disable the exit track menu
    const val KEY_SMAP_BG_STOP_MENU = "smap_bg_stop_menu" // Disable the exit track menu
    const val KEY_SMAP_OVERRIDE_SYNC = "smap_override_sync" // Override the local settings for synchronisation
    const val KEY_SMAP_OVERRIDE_LOCATION = "smap_override_location" // Override the local settings for user trail
    const val KEY_SMAP_OVERRIDE_DELETE = "smap_override_del" // Override the local settings for delete after send
    const val KEY_SMAP_OVERRIDE_HIGH_RES_VIDEO = "smap_override_high_res_video" // Override the local settings for video resolution
    const val KEY_SMAP_OVERRIDE_GUIDANCE = "smap_override_guidance" // Override the local settings for guidance hint
    const val KEY_SMAP_OVERRIDE_IMAGE_SIZE = "smap_override_image_size" // Override the local settings for the image size
    const val KEY_SMAP_OVERRIDE_NAVIGATION = "smap_override_navigation" // Override the local settings for the screen navigation
    const val KEY_SMAP_REGISTRATION_ID = "registration_id" // Android notifications id
    const val KEY_SMAP_REGISTRATION_SERVER = "registration_server" // Server name that has been registered
    const val KEY_SMAP_REGISTRATION_USER = "registration_user" // User name that has been registered
    const val KEY_SMAP_LAST_LOGIN = "last_login" // System time in milli seconds that the user last logged in
    const val KEY_SMAP_PASSWORD_POLICY = "pw_policy"
    const val KEY_SMAP_CURRENT_ORGANISATION = "smap_current_organisation"
    const val KEY_SMAP_ORGANISATIONS = "smap_organisations"
    const val KEY_SMAP_INPUT_METHOD = "smap_input_method" // GeoPoly recording settings
    const val KEY_SMAP_IM_RI = "smap_im_ri"
    const val KEY_SMAP_IM_ACC = "smap_im_acc"
    const val KEY_SMAP_REQUEST_LOCATION_DONE = "smap_request_location_done"
}
