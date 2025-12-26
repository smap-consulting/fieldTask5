/*
 * Copyright (C) 2017 Smap Consulting Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package au.smap.fieldTask.activities;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import org.odk.collect.android.utilities.ApplicationConstants;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import org.odk.collect.material.MaterialProgressDialogFragment;

import org.odk.collect.android.R;
import au.smap.fieldTask.viewmodels.SurveyDataViewModel;
import au.smap.fieldTask.viewmodels.SurveyDataViewModelFactory;
import au.smap.fieldTask.adapters.ViewPagerAdapter;
import org.odk.collect.android.application.Collect;
import au.smap.fieldTask.fragments.SmapFormListFragment;
import au.smap.fieldTask.fragments.SmapTaskListFragment;
import au.smap.fieldTask.fragments.SmapTaskMapFragment;
import au.smap.fieldTask.fragments.dialogs.RequestLocationPermissionsDialogSmap;
import org.odk.collect.android.formmanagement.FormFillingIntentFactory;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.listeners.InstanceUploaderListener;
import org.odk.collect.android.projects.ProjectsDataService;

import au.smap.fieldTask.listeners.NFCListener;
import au.smap.fieldTask.listeners.TaskDownloaderListener;
import au.smap.fieldTask.loaders.SurveyData;
import au.smap.fieldTask.loaders.TaskEntry;
import org.odk.collect.permissions.PermissionsProvider;
import org.odk.collect.settings.keys.ProtectedProjectKeys;
import au.smap.fieldTask.preferences.AdminPreferencesActivitySmap;
import org.odk.collect.settings.keys.ProjectKeys;
import au.smap.fieldTask.preferences.GeneralSharedPreferencesSmap;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI;
import au.smap.fieldTask.services.LocationService;
import au.smap.fieldTask.formmanagement.ServerFormDetailsSmap;
import au.smap.fieldTask.listeners.DownloadFormsTaskListenerSmap;
import org.odk.collect.android.smap.utilities.LocationRegister;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.storage.StorageSubdirectory;
import au.smap.fieldTask.models.FormLaunchDetail;
import au.smap.fieldTask.models.FormRestartDetails;
import au.smap.fieldTask.models.NfcTrigger;
import au.smap.fieldTask.tasks.DownloadTasksTask;
import au.smap.fieldTask.tasks.NdefReaderTask;
import au.smap.fieldTask.utilities.ManageForm;
import org.odk.collect.androidshared.ui.multiclicksafe.MultiClickGuard;
import org.odk.collect.androidshared.ui.SnackbarUtils;
import au.smap.fieldTask.utilities.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.odk.collect.android.databinding.SmapMainLayoutBinding;
import timber.log.Timber;

public class SmapMain extends CollectAbstractActivity implements TaskDownloaderListener,
        NFCListener,
        InstanceUploaderListener,
        DownloadFormsTaskListenerSmap {

    private static final String PROGRESS_DIALOG_TAG = "progressDialog";
    private static final int COMPLETE_FORM = 4;

    private String mAlertMsg;
    private boolean mPaused = false;

    public static final String EXTRA_REFRESH = "refresh";
    public static final String LOGIN_STATUS = "login_status";

    private final SmapFormListFragment formManagerList = SmapFormListFragment.newInstance();
    private final SmapTaskListFragment taskManagerList = SmapTaskListFragment.newInstance();
    private final SmapTaskMapFragment taskManagerMap = SmapTaskMapFragment.newInstance();

    private NfcAdapter mNfcAdapter;        // NFC
    public PendingIntent mNfcPendingIntent;
    public IntentFilter[] mNfcFilters;
    public NdefReaderTask mReadNFC;
    public ArrayList<NfcTrigger> nfcTriggersList;   // nfcTriggers (geofence should have separate list)

    private String mProgressMsg;
    public DownloadTasksTask mDownloadTasks;

    SurveyDataViewModel model;
    private MainTaskListener listener = null;
    private RefreshListener refreshListener = null;

    boolean listenerRegistered = false;
    private static List<TaskEntry> mTasks = null;

    private Intent mLocationServiceIntent = null;
    private LocationService mLocationService = null;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;

    private SmapMainLayoutBinding binding;

    @Inject
    PermissionsProvider permissionsProvider;

    @Inject
    ProjectsDataService projectsDataService;

    @Inject
    org.odk.collect.projects.ProjectsRepository projectsRepository;

    /*
     * Start scoped storage
     */

    @Inject
    StoragePathProvider storagePathProvider;

    // End scoped storage


    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setTitle(getString(org.odk.collect.strings.R.string.app_name));
        toolbar.setNavigationIcon(R.mipmap.ic_nav);
        setSupportActionBar(toolbar);
    }

    private void initSplashScreen() {
        /*
        We don't need the installSplashScreen call on Android 12+ (the system handles the
        splash screen for us) and it causes problems if we later switch between dark/light themes.
         */
        if (Build.VERSION.SDK_INT < 31) {
            SplashScreen.installSplashScreen(this);
        } else {
            setTheme(R.style.Theme_Collect);
        }
    }

    private void ensureDefaultProject() {
        // Check if any projects exist
        if (projectsRepository.getAll().isEmpty()) {
            // Create a default project
            org.odk.collect.projects.Project.New defaultProject = new org.odk.collect.projects.Project.New(
                    "Default",  // name
                    "D",        // icon
                    "#3e9fcc"   // color (ODK blue)
            );
            org.odk.collect.projects.Project.Saved savedProject = projectsRepository.save(defaultProject);
            projectsDataService.setCurrentProject(savedProject.getUuid());
        } else if (projectsDataService.getCurrentProject() == null) {
            // Projects exist but no current project is set - set the first one as current
            org.odk.collect.projects.Project.Saved firstProject = projectsRepository.getAll().get(0);
            projectsDataService.setCurrentProject(firstProject.getUuid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initSplashScreen();
        super.onCreate(savedInstanceState);
        binding = SmapMainLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        LocationRegister lr = new LocationRegister();
        DaggerUtils.getComponent(this).inject(this);

        // Ensure a default project exists before proceeding
        ensureDefaultProject();

        String[] tabNames = {getString(R.string.smap_forms), getString(R.string.smap_tasks), getString(R.string.smap_map)};
        // Get the ViewPager and set its PagerAdapter so that it can display items
        binding.pager.setOffscreenPageLimit(2);

        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(formManagerList);
        fragments.add(taskManagerList);
        fragments.add(taskManagerMap);

        binding.pager.setAdapter(new ViewPagerAdapter(
                getSupportFragmentManager(), tabNames, fragments));

        // Give the SlidingTabLayout the ViewPager
        // Attach the view pager to the tab strip
        binding.tabs.setBackgroundColor(getResources().getColor(R.color.tabBackground));

        binding.tabs.setTabTextColors(Color.LTGRAY, Color.WHITE);
        binding.tabs.setupWithViewPager(binding.pager);
        binding.tabs.setTabGravity(TabLayout.GRAVITY_FILL);
        binding.tabs.setTabMode(TabLayout.MODE_FIXED);

        stateChanged();

        // Show login status if it was set
        String login_status = getIntent().getStringExtra(LOGIN_STATUS);
        if(login_status != null) {
            if(login_status.equals("success")) {
                SnackbarUtils.showSnackbar(binding.pager, Collect.getInstance().getString(R.string.smap_login_success), SnackbarUtils.DURATION_SHORT);
                Utilities.updateServerRegistration(false);     // Update the server registration
            } else if(login_status.equals("failed")) {
                SnackbarUtils.showSnackbar(binding.pager, Collect.getInstance().getString(R.string.smap_login_failed), SnackbarUtils.DURATION_SHORT);
            }
        }

        // Restore the preference to record a user trail in case the user had previously selected "exit"
        GeneralSharedPreferencesSmap.getInstance().save(ProjectKeys.KEY_SMAP_USER_LOCATION,
                GeneralSharedPreferencesSmap.getInstance().getBoolean(ProjectKeys.KEY_SMAP_USER_SAVE_LOCATION, false));

        // Initiate a refresh if requested in start parameters
        String refresh = getIntent().getStringExtra(EXTRA_REFRESH);
        if(refresh != null && refresh.equals("yes")) {
            processGetTask(true);   // Set manual true so that refresh after logon works (logon = manual refresh request)
        }

        /*
         * Show a notice if location recording has not been granted
         */
        boolean hasFineLocation = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED;
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED;
        String asked = (String) GeneralSharedPreferencesSmap.getInstance().get(ProjectKeys.KEY_SMAP_REQUEST_LOCATION_DONE);
        if (asked != null && asked.equals("no")) {
            (new RequestLocationPermissionsDialogSmap()).show(this.getSupportFragmentManager(), RequestLocationPermissionsDialogSmap.TAG);
        } else if ((hasFineLocation || hasCoarseLocation) && ("accept".equals(asked))){
            lr.locationStart(this, permissionsProvider);
        }

        try {
            lr.isValidInstallation(this);
        } catch (Exception e) {
            createErrorDialog(e.getMessage(), true);
        }
    }

    public SurveyDataViewModel getViewModel() {
        return model;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_nav);
        stateChanged();
    }

    /*
     * Start a foreground service
     */
    public void startLocationService() {

        mLocationService = new LocationService();
        mLocationServiceIntent = new Intent(Collect.getInstance().getApplicationContext(), mLocationService.getClass());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(mLocationServiceIntent);
        } else {
            startService(mLocationServiceIntent);
        }
        taskManagerMap.permissionsGranted();
    }

    /*
     * Do all the actions required on create or rotate
     */
    private void stateChanged() {

        initToolbar();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SurveyDataViewModelFactory viewModelFactory = new SurveyDataViewModelFactory(sharedPreferences);

        model = new ViewModelProvider(this, viewModelFactory).get(SurveyDataViewModel.class);
        model.getSurveyData().observe(this, surveyData -> {
            Timber.d("SmapMain: Survey data updated");
            updateData(surveyData);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPaused = false;

        if (!listenerRegistered) {
            listener = new MainTaskListener(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction("startTask");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(listener, filter, Context.RECEIVER_NOT_EXPORTED);
            }

            refreshListener = new RefreshListener(this);   // Listen for updates to the form list

            listenerRegistered = true;
        }

        // NFC
        boolean nfcAuthorised = false;
        SharedPreferences sharedPreferences = this.getSharedPreferences(
                AdminPreferencesActivitySmap.ADMIN_PREFERENCES, 0);

        if (sharedPreferences.getBoolean(ProjectKeys.KEY_SMAP_LOCATION_TRIGGER, true)) {
            if(mNfcAdapter == null) {
                mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            }

            if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {

                // Pending intent
                Intent nfcIntent = new Intent(getApplicationContext(), getClass());
                nfcIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                if(mNfcPendingIntent == null) {
                    mNfcPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, nfcIntent,
                            PendingIntent.FLAG_MUTABLE);    // Must be mutable
                }

                if(mNfcFilters == null) {
                    // Filter
                    IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                    mNfcFilters = new IntentFilter[]{
                            filter
                    };
                }

                mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mNfcFilters, null);

            }
        }

    }

    @Override
    protected void onDestroy() {
        if(mLocationService != null) {
            stopService(mLocationServiceIntent);
        }
        super.onDestroy();

    }

    public void processAdminMenu() {
        showPasswordDialog();
    }

    // Get tasks and forms from the server
    public void processGetTask(boolean manual) {

      if(manual || Utilities.isFormAutoSendOptionEnabled()) {
            mDownloadTasks = new DownloadTasksTask();
            if(manual) {
                mProgressMsg = getString(R.string.smap_synchronising);
                if (!this.isFinishing()) {
                    showProgressDialog(mProgressMsg);
                }
                mDownloadTasks.setDownloaderListener(this, this);
            }
            mDownloadTasks.execute();
        }
    }

    public void processHistory() {
        if (MultiClickGuard.allowClick(getClass().getName())) {
            Intent i = new Intent(getApplicationContext(), HistoryActivity.class);
            i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                    ApplicationConstants.FormModes.VIEW_SENT);
            startActivity(i);
        }
    }

    /**
     * Show modern progress dialog with cancel support
     */
    private void showProgressDialog(String message) {
        MaterialProgressDialogFragment progressDialog = new MaterialProgressDialogFragment();
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show(getSupportFragmentManager(), PROGRESS_DIALOG_TAG);
    }

    /**
     * Dismiss progress dialog if showing
     */
    private void dismissProgressDialog() {
        MaterialProgressDialogFragment dialog =
            (MaterialProgressDialogFragment) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Show admin password dialog using modern Material design
     */
    private void showPasswordDialog() {
        final SharedPreferences adminPreferences = getSharedPreferences(
                AdminPreferencesActivitySmap.ADMIN_PREFERENCES, 0);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());

        new MaterialAlertDialogBuilder(this)
                .setTitle(org.odk.collect.strings.R.string.enter_admin_password)
                .setView(input, 20, 10, 20, 10)
                .setPositiveButton(org.odk.collect.strings.R.string.ok, (dialog, which) -> {
                    String value = input.getText().toString();
                    String pw = adminPreferences.getString(ProtectedProjectKeys.KEY_ADMIN_PW, "");
                    if (pw.equals(value)) {
                        Intent i = new Intent(getApplicationContext(), AdminPreferencesActivitySmap.class);
                        startActivity(i);
                        input.setText("");
                    } else {
                        Toast.makeText(
                                SmapMain.this,
                                getString(org.odk.collect.strings.R.string.admin_password_incorrect),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(org.odk.collect.strings.R.string.cancel, (dialog, which) -> {
                    input.setText("");
                })
                .create()
                .show();

        // Show keyboard
        input.post(() -> {
            input.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    /*
     * Forms Downloading Overrides
     */
    @Override
    public void formsDownloadingComplete(Map<ServerFormDetailsSmap, String> result) {
        // Ignore formsDownloading is called synchronously from taskDownloader
    }

    @Override
    public void progressUpdate(String currentFile, int progress, int total) {
        mProgressMsg = getString(R.string.smap_checking_file, currentFile, String.valueOf(progress), String.valueOf(total));
        if (!isFinishing() && !isDestroyed()) {
            MaterialProgressDialogFragment dialog =
                (MaterialProgressDialogFragment) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
            if (dialog != null) {
                dialog.setMessage(mProgressMsg);
            }
        }
    }

    @Override
    public void formsDownloadingCancelled() {
       // ignore
    }

    /*
     * Task Download overrides
     */
    @Override
    // Download tasks progress update
    public void progressUpdate(String progress) {
        if(mProgressMsg != null && !isFinishing() && !isDestroyed()) {
            mProgressMsg = progress;
            MaterialProgressDialogFragment dialog =
                (MaterialProgressDialogFragment) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
            if (dialog != null) {
                dialog.setMessage(mProgressMsg);
            }
        }
    }

    @Override
    public void taskDownloadingComplete(HashMap<String, String> result) {

        Timber.i("Complete - Send intent");

        try {
            dismissProgressDialog();
        } catch (IllegalArgumentException e) {
            // Dialog not showing - expected, ignore
        } catch (Exception e) {
            Timber.w(e, "Unexpected error dismissing dialog");
        }

        if (result != null) {
            StringBuilder message = new StringBuilder();
            Set<String> keys = result.keySet();
            Iterator<String> it = keys.iterator();

            while (it.hasNext()) {
                String key = it.next();
                String m = result.get(key);
                if (key.equals("err_not_enabled")) {
                    message.append(this.getString(R.string.smap_tasks_not_enabled));
                } else if (key.equals("err_no_tasks")) {
                    // No tasks is fine, in fact its the most common state
                } else if (key.equals("Error:") && m != null && m.startsWith("403")) {
                    message.append(this.getString(R.string.smap_unauth));
                } else {
                    message.append(key).append(" - ").append(m).append("\n\n");
                }
            }

            mAlertMsg = message.toString().trim();
            if (mAlertMsg.length() > 0) {
                try {
                    if (!isFinishing() && !isDestroyed()) {
                        new MaterialAlertDialogBuilder(SmapMain.this)
                                .setTitle(R.string.smap_get_tasks)
                                .setMessage(mAlertMsg)
                                .setCancelable(true)
                                .setNeutralButton(org.odk.collect.strings.R.string.ok, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                } catch (Exception e) {
                    Timber.e(e);
                    // Tried to show a dialog but the activity may have been closed
                }
            }

        }
    }

    /*
     * Uploading overrides
     */
    @Override
    public void uploadingComplete(HashMap<String, String> result) {
        // Upload complete - no action needed here
    }

    @Override
    public void progressUpdate(int progress, int total) {
        mAlertMsg = getString(org.odk.collect.strings.R.string.sending_items, String.valueOf(progress), String.valueOf(total));
        if (!isFinishing() && !isDestroyed()) {
            MaterialProgressDialogFragment dialog =
                (MaterialProgressDialogFragment) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
            if (dialog != null) {
                dialog.setMessage(mAlertMsg);
            }
        }
    }

    @Override
    public void authRequest(Uri url, HashMap<String, String> doneSoFar) {
        // Auth request - handled by parent class
    }

    /*
     * NFC Reading Overrides
     */


    /**
     * @param activity The corresponding {@link Activity} requesting to stop the foreground dispatch.
     * @param adapter  The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopNFCDispatch(final Activity activity, NfcAdapter adapter) {

        if (adapter != null) {
            adapter.disableForegroundDispatch(activity);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNFCIntent(intent);
    }

    /*
     * NFC detected
     */
    private void handleNFCIntent(Intent intent) {

        if (nfcTriggersList != null && nfcTriggersList.size() > 0) {
            Timber.i("tag discovered");
            String action = intent.getAction();
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            mReadNFC = new NdefReaderTask();
            mReadNFC.setDownloaderListener(this);
            mReadNFC.execute(tag);
        } else {
            Toast.makeText(
                    this,
                    R.string.smap_no_tasks_nfc,
                    Toast.LENGTH_LONG).show();
        }

    }


    @Override
    public void readComplete(String result) {

        boolean foundTask = false;

        if (nfcTriggersList != null) {
            for (NfcTrigger trigger : nfcTriggersList) {
                if (trigger.uid.equals(result)) {
                    foundTask = true;

                    Intent i = new Intent();
                    i.setAction("startTask");
                    i.setPackage(getPackageName());
                    i.putExtra("position", trigger.position);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(i);

                    Toast.makeText(
                            SmapMain.this,
                            getString(R.string.smap_starting_task_from_nfc, result),
                            Toast.LENGTH_LONG).show();

                    break;
                }
            }
        }
        if (!foundTask) {
            Toast.makeText(
                    SmapMain.this,
                    getString(R.string.smap_no_matching_tasks_nfc, result),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == COMPLETE_FORM && intent != null) {

            String instanceId = intent.getStringExtra("instanceid");
            String formStatus = intent.getStringExtra("status");
            String formURI = intent.getStringExtra("uri");

            formCompleted(instanceId, formStatus, formURI);
        }
    }

    /*
     * The user has selected an option to edit / complete a task
     * If the activity has been paused then a task has already been launched so ignore
     * Unless this request comes not from a user click but from code in which case force the launch
     */
    public void completeTask(TaskEntry entry, boolean force) {

        if(!mPaused || force) {
            String surveyNotes = null;
            String formPath = new StoragePathProvider().getDirPath(StorageSubdirectory.FORMS) + entry.taskForm;
            String instancePath = entry.instancePath;
            long taskId = entry.id;
            String status = entry.taskStatus;

            // set the adhoc location
            boolean canUpdate = Utilities.canComplete(status, entry.taskType);
            boolean isSubmitted = Utilities.isSubmitted(status);
            boolean isSelfAssigned = Utilities.isSelfAssigned(status);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean reviewFinal = sharedPreferences.getBoolean(ProjectKeys.KEY_SMAP_REVIEW_FINAL, true);

            if (isSubmitted) {
                Toast.makeText(
                        SmapMain.this,
                        getString(R.string.smap_been_submitted),
                        Toast.LENGTH_LONG).show();
            } else if (!canUpdate && reviewFinal) {
                // Show a message if this task is read only
                if(isSelfAssigned) {
                    Toast.makeText(
                            SmapMain.this,
                            getString(R.string.smap_self_select),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(
                            SmapMain.this,
                            getString(R.string.read_only),
                            Toast.LENGTH_LONG).show();
                }
            } else if (!canUpdate && !reviewFinal) {
                // Show a message if this task is read only and cannot be reviewed
                Toast.makeText(
                        SmapMain.this,
                        getString(R.string.no_review),
                        Toast.LENGTH_LONG).show();
            }

            // Open the task if it is editable or reviewable
            if ((canUpdate || reviewFinal) && !isSubmitted && !isSelfAssigned) {
                // Get the provider URI of the instance
                String where = InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH + "=?";
                String[] whereArgs = {
                        instancePath
                };

                Timber.i("Complete Task: " + entry.id + " : " + entry.name + " : "
                        + entry.taskStatus + " : " + instancePath);

                Cursor cInstanceProvider = Collect.getInstance().getContentResolver().query(InstanceProviderAPI.InstanceColumns.CONTENT_URI,
                        null, where, whereArgs, null);

                if (entry.repeat) {
                    entry.instancePath = duplicateInstance(formPath, entry.instancePath, entry);
                }

                if (cInstanceProvider.moveToFirst()) {
                    long idx = cInstanceProvider.getLong(cInstanceProvider.getColumnIndexOrThrow(InstanceProviderAPI.InstanceColumns._ID));
                    if (idx > 0) {
                        surveyNotes = cInstanceProvider.getString(
                                cInstanceProvider.getColumnIndexOrThrow(InstanceProviderAPI.InstanceColumns.T_SURVEY_NOTES));
                        // Start activity to complete form

                        // Use FormFillingIntentFactory to create the intent
                        String projectId = projectsDataService.requireCurrentProject().getUuid();
                        Intent i = FormFillingIntentFactory.editDraftFormIntent(this, projectId, idx);

                        // Add Smap-specific extras
                        i.putExtra(org.odk.collect.android.activities.FormFillingActivity.KEY_TASK, taskId);
                        i.putExtra(org.odk.collect.android.activities.FormFillingActivity.KEY_SURVEY_NOTES, surveyNotes);
                        i.putExtra(org.odk.collect.android.activities.FormFillingActivity.KEY_CAN_UPDATE, canUpdate);
                        i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
                        if (entry.formIndex != null) {
                            FormRestartDetails frd = new FormRestartDetails();
                            frd.initiatingQuestion = entry.formIndex;
                            frd.launchedFormStatus = entry.formStatus;
                            frd.launchedFormInstanceId = entry.instanceId;
                            frd.launchedFormURI = entry.formURI;
                            Collect.getInstance().setFormRestartDetails(frd);
                        }
                        if (instancePath != null) {
                            i.putExtra(org.odk.collect.android.activities.FormFillingActivity.KEY_INSTANCEPATH, instancePath);
                        }
                        startActivityForResult(i, COMPLETE_FORM);

                        // If more than one instance is found pointing towards a single file path then report the error
                        int instanceCount = cInstanceProvider.getCount();
                        if (instanceCount > 1) {
                            Timber.e("Unique instance not found: found %d instances for path: %s", 
                                    instanceCount, instancePath);
                            // TODO: Implement cleanup of duplicate instances to prevent data corruption
                        }
                    }
                } else {
                    Timber.e("Task not found for instance path: %s", instancePath);
                }

                cInstanceProvider.close();
            }
        } else {
            Timber.d("Task launch blocked: activity is paused");
        }

    }

    /*
     * The user has selected an option to edit / complete a form
     * The force parameter can be used to force launching of the new form even when the smap activity is paused
     */
    public void completeForm(TaskEntry entry, boolean force, String initialData) {
        if(!mPaused || force) {
            String projectId = projectsDataService.requireCurrentProject().getUuid();
            Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, entry.id);

            // Use FormFillingIntentFactory to create the intent
            Intent i = FormFillingIntentFactory.newFormIntent(this, formUri);

            // Add Smap-specific extras
            i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
            i.putExtra(org.odk.collect.android.activities.FormFillingActivity.KEY_READ_ONLY, entry.readOnly);
            if(initialData != null) {
                i.putExtra(org.odk.collect.android.activities.FormFillingActivity.KEY_INITIAL_DATA, initialData);
            }
            startActivityForResult(i, COMPLETE_FORM);
        } else {
            Timber.d("Form launch blocked: activity is paused");
        }
    }

    /*
     * respond to completion of a form
     */
    public void formCompleted(String instanceId, String formStatus, String formURI) {
        Timber.i("Form completed");
        FormLaunchDetail fld = Collect.getInstance().popFromFormStack();
        TaskEntry te = new TaskEntry();
        if(fld != null) {
            if(fld.id > 0) {
                // Start a form
                te.id = fld.id;

                SnackbarUtils.showSnackbar(binding.rl,
                        Collect.getInstance().getString(R.string.smap_starting_form, fld.formName),
                        SnackbarUtils.DURATION_LONG);

                completeForm(te, true, fld.initialData);
            } else if(fld.instancePath != null) {
                // Start a task or saved instance
                te.id = 0;
                te.instancePath = fld.instancePath;
                te.taskStatus = Utilities.STATUS_T_ACCEPTED;
                te.repeat = false;
                te.formIndex = fld.formIndex;
                te.instanceId = instanceId;
                te.formStatus = formStatus;
                te.formURI = formURI;

                SnackbarUtils.showSnackbar(binding.pager,
                        Collect.getInstance().getString(R.string.smap_restarting_form, fld.formName),
                        SnackbarUtils.DURATION_LONG);

                completeTask(te, true);
            }
        } else {
            if(formStatus != null && formStatus.equals("complete")) {
                processGetTask(false);
            }
        }
    }

    /*
     * Duplicate the instance
     * Call this if the instance repeats
     */
    public String duplicateInstance(String formPath, String originalPath, TaskEntry entry) {
        String newPath = null;

        // 1. Get a new instance path
        ManageForm mf = new ManageForm();
        newPath = mf.getInstancePath(formPath, entry.assId, null);

        // 2. Duplicate the instance entry and get the new path
        Utilities.duplicateTask(originalPath, newPath, entry);

        // 3. Copy the instance files
        Utilities.copyInstanceFiles(originalPath, newPath, formPath);
        return newPath;
    }

    /*
     * Get the tasks shown on the map
     */
    public List<TaskEntry> getTasks() {
        return mTasks;
    }

    /*
     * Manage location triggers
     */
    public void setLocationTriggers(List<TaskEntry> data) {

        mTasks = data;
        nfcTriggersList = new ArrayList<NfcTrigger>();

        /*
         * Set NFC triggers
         */

        int position = 0;
        for (TaskEntry t : data) {
            if (t.type.equals("task") && t.locationTrigger != null && t.locationTrigger.trim().length() > 0
                    && t.taskStatus.equals(Utilities.STATUS_T_ACCEPTED)) {
                nfcTriggersList.add(new NfcTrigger(t.id, t.locationTrigger, position));
            }
            position++;
        }

    }

    /*
     * Update fragments that use data sourced from the loader that called this method
     */
    public void updateData(SurveyData data) {
        formManagerList.setData(data); // loader
        taskManagerList.setData(data);
        taskManagerMap.setData(data);
        if(data != null) {
            setLocationTriggers(data.tasks);      // NFC and geofence triggers
        }
    }

    public void locateTaskOnMap(TaskEntry task) {
        taskManagerMap.locateTask(task);
        binding.pager.setCurrentItem(2);
    }

    protected class MainTaskListener extends BroadcastReceiver {

        private SmapMain mActivity = null;

        public MainTaskListener(SmapMain activity) {
            mActivity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            Timber.i("Intent received: %s", intent.getAction());

            if (intent.getAction().equals("startTask")) {

                int position = intent.getIntExtra("position", -1);
                if (position >= 0) {
                    TaskEntry entry = (TaskEntry) mTasks.get(position);

                    mActivity.completeTask(entry, true);
                }
            }
        }
    }

    /*
     * The user has chosen to exit the application
     */
    public void exit() {
        boolean continueTracking = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(ProjectKeys.KEY_SMAP_EXIT_TRACK_MENU, false);
        if(!continueTracking) {
            GeneralSharedPreferencesSmap.getInstance().save(ProjectKeys.KEY_SMAP_USER_LOCATION, false);
            this.finish();
        } else {
            SnackbarUtils.showSnackbar(binding.pager,
                    Collect.getInstance().getString(R.string.smap_continue_tracking),
                    SnackbarUtils.DURATION_LONG);
        }

    }

    @Override
    protected void onPause() {
        mPaused = true;
        super.onPause();

        if (listener != null) {
            try {
                unregisterReceiver(listener);
                listener = null;
            } catch (Exception e) {
                // Ignore - presumably already unregistered
            }
        }

        if (refreshListener != null) {
            try {
                unregisterReceiver(refreshListener);
                refreshListener = null;
            } catch (Exception e) {
                // Ignore - presumably already unregistered
            }
        }
        listenerRegistered = false;
    }

    protected class RefreshListener extends BroadcastReceiver {

        public RefreshListener (Context context) {
            LocalBroadcastManager.getInstance(context).registerReceiver(this,
                    new IntentFilter("org.smap.smapTask.refresh"));
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            Timber.i("Intent received: %s", intent.getAction());

            model.loadData();
        }
    }

    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        new MaterialAlertDialogBuilder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setMessage(errorMsg)
            .setCancelable(false)
            .setPositiveButton(org.odk.collect.strings.R.string.ok, (dialog, which) -> {
                if (shouldExit) {
                    finish();
                }
            })
            .show();
    }
}
