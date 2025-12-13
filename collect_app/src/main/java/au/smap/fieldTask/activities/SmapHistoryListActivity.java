package au.smap.fieldTask.activities;

import org.odk.collect.android.activities.AppListActivity;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;

abstract class SmapHistoryListActivity extends AppListActivity {
    protected String getSortingOrder() {
        return InstanceColumns.LAST_STATUS_CHANGE_DATE + " DESC";
    }
}