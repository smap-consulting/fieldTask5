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

package au.smap.fieldTask.fragments;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import org.odk.collect.android.R;
import au.smap.fieldTask.adapters.SortDialogAdapter;
import au.smap.fieldTask.utilities.SystemLocationProvider;
import org.odk.collect.android.activities.AboutActivity;
import org.odk.collect.android.activities.FormDownloadListActivity;
import au.smap.fieldTask.activities.SmapMain;
import au.smap.fieldTask.viewmodels.SurveyDataViewModel;
import au.smap.fieldTask.adapters.TaskRecyclerAdapter;
import org.odk.collect.android.database.instances.DatabaseInstancesRepository;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.forms.instances.Instance;
import au.smap.fieldTask.listeners.OnTaskOptionsClickListener;
import au.smap.fieldTask.loaders.SurveyData;
import au.smap.fieldTask.loaders.TaskEntry;
import org.odk.collect.settings.keys.ProtectedProjectKeys;
import au.smap.fieldTask.preferences.AdminPreferencesActivitySmap;
import org.odk.collect.settings.keys.ProjectKeys;
import org.odk.collect.shared.settings.Settings;
import org.odk.collect.android.preferences.screens.ProjectPreferencesActivity;
import org.odk.collect.android.smap.utilities.LocationRegister;
import org.odk.collect.androidshared.ui.multiclicksafe.MultiClickGuard;
import org.odk.collect.androidshared.ui.SnackbarUtils;
import org.odk.collect.android.utilities.ThemeUtils;

import java.util.Locale;

import au.smap.fieldTask.utilities.Utilities;

import timber.log.Timber;

/**
 * Responsible for displaying tasks on the main fieldTask screen
 */
public class SmapTaskListFragment extends Fragment {

    private static final int MENU_ENTERDATA = Menu.FIRST + 2;
    private static final int MENU_GETFORMS = Menu.FIRST + 3;
    private static final int MENU_SENDDATA = Menu.FIRST + 4;
    private static final int MENU_MANAGEFILES = Menu.FIRST + 5;
    private static final int MENU_EXIT = Menu.FIRST + 6;
    private static final int MENU_HISTORY = Menu.FIRST + 7;

    protected int[] sortingOptions;

    View rootView;

    private String filterText;

    private BottomSheetDialog bottomSheetDialog;

    private OnTaskOptionsClickListener taskClickListener;
    private TaskRecyclerAdapter mAdapter;
    private RecyclerView recyclerView;
    private View emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private com.google.android.material.button.MaterialButtonToggleGroup filterGroup;
    private com.google.android.material.button.MaterialButton btnTasks;
    private com.google.android.material.button.MaterialButton btnReferences;
    private java.util.List<TaskEntry> allTasks;     // Full task list before the references filter
    private boolean showReferences;                 // True when the References segment is selected

    SurveyDataViewModel model;

    public static SmapTaskListFragment newInstance() {
        return new SmapTaskListFragment();
    }

    public SmapTaskListFragment() {
    }

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.smap_task_recycler_layout, container, false);

        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);

        taskClickListener = new OnTaskOptionsClickListener() {
            final DatabaseInstancesRepository di = new DatabaseInstancesRepository();

            @Override
            public void onAcceptClicked(TaskEntry taskEntry) {
                if (Utilities.canAccept(taskEntry.taskStatus)) {
                    Utilities.setStatusForTask(taskEntry.id, Utilities.STATUS_T_ACCEPTED, "");
                    Intent intent = new Intent("org.smap.smapTask.refresh");      // Notify map and task list of change
                    LocalBroadcastManager.getInstance(requireActivity().getApplication()).sendBroadcast(intent);
                    Timber.i("######## send org.smap.smapTask.refresh from instanceUploaderActivity2");
                } else {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setMessage(getString(R.string.smap_cannot_accept))
                            .show();
                }
            }

            @Override
            public void onSMSClicked(TaskEntry taskEntry) {
                Instance instance = di.getInstanceByTaskId(taskEntry.assId);
                String number = null;
                if (instance != null) {
                    number = instance.getPhone();
                }
                if (number != null) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, null)));
                } else {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setMessage(requireContext().getString(R.string.smap_phone_number_not_found))
                            .show();
                }
            }

            @Override
            public void onPhoneClicked(TaskEntry taskEntry) {
                Instance instance = di.getInstanceByTaskId(taskEntry.assId);
                if (instance != null) {
                    String number = instance.getPhone();
                    if (number != null) {
                        Intent callIntent = new Intent(Intent.ACTION_DIAL);
                        callIntent.setData(Uri.parse("tel:" + number));
                        startActivity(callIntent);
                    } else {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setMessage(requireContext().getString(R.string.smap_phone_number_not_found))
                                .show();
                    }
                }
            }

            @Override
            public void onRejectClicked(TaskEntry taskEntry) {
                View reject_popup = getLayoutInflater().inflate(R.layout.reject_task, null);
                androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                        .setView(reject_popup)
                        .create();
                dialog.show();
                EditText editText = reject_popup.findViewById(R.id.input_reason);
                Button ok = reject_popup.findViewById(R.id.ok);
                Button cancel = reject_popup.findViewById(R.id.cancel);

                if(taskEntry.taskType != null && taskEntry.taskType.equals("case")) {
                    TextView titleText = reject_popup.findViewById(R.id.reject_title);
                    titleText.setText(getContext().getString(R.string.smap_release_case));
                }
                ok.setOnClickListener(view -> {
                    String reason = editText.getText().toString();
                    rejectTask(reason, taskEntry);
                    dialog.dismiss();
                });
                cancel.setOnClickListener(view -> dialog.dismiss());
            }

            @Override
            public void onDirectionsClicked(TaskEntry taskEntry) {
                String uri = String.format(Locale.ROOT,
                        "geo:0,0?q=%f,%f (%s)",
                        taskEntry.schedLat,
                        taskEntry.schedLon,
                        taskEntry.name
                );
                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(uri)
                );
                startActivity(intent);
            }

            @Override
            public void onLocateClick(TaskEntry taskEntry) {
                SmapMain activity = ((SmapMain) requireActivity());
                activity.locateTaskOnMap(taskEntry);
            }
        };

        mAdapter = new TaskRecyclerAdapter(getActivity(), taskClickListener, this::onRowClick);

        recyclerView = rootView.findViewById(R.id.task_recycler);
        emptyView = rootView.findViewById(android.R.id.empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mAdapter);

        new ItemTouchHelper(new TaskSwipeCallback()).attachToRecyclerView(recyclerView);
    }

    /*
     * Swipe left or right on an actionable task/case row to reject (task) or release (case).
     * Reuses the existing reject/release reason dialog. Form and reference rows are not swipeable.
     */
    private class TaskSwipeCallback extends ItemTouchHelper.SimpleCallback {

        private final Paint background = new Paint();
        private final Drawable icon;
        private final int iconMargin;

        TaskSwipeCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            background.setColor(Color.parseColor("#D32F2F"));
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.form_state_rejected);
            iconMargin = (int) (16 * getResources().getDisplayMetrics().density);
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
            int pos = vh.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || mAdapter == null || !mAdapter.isSwipeable(pos)) {
                return 0;
            }
            return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
            int pos = vh.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || mAdapter == null) {
                return;
            }
            TaskEntry entry = mAdapter.getItem(pos);
            // Spring the row back; the reject/release dialog confirms the action and a refresh
            // broadcast reloads the list when it actually changes.
            mAdapter.notifyItemChanged(pos);
            taskClickListener.onRejectClicked(entry);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                @NonNull RecyclerView.ViewHolder vh, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
            View item = vh.itemView;
            if (dX > 0) {
                c.drawRect(item.getLeft(), item.getTop(), item.getLeft() + dX, item.getBottom(), background);
                if (icon != null) {
                    int top = item.getTop() + (item.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int left = item.getLeft() + iconMargin;
                    icon.setBounds(left, top, left + icon.getIntrinsicWidth(), top + icon.getIntrinsicHeight());
                    icon.draw(c);
                }
            } else if (dX < 0) {
                c.drawRect(item.getRight() + dX, item.getTop(), item.getRight(), item.getBottom(), background);
                if (icon != null) {
                    int top = item.getTop() + (item.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int right = item.getRight() - iconMargin;
                    icon.setBounds(right - icon.getIntrinsicWidth(), top, right, top + icon.getIntrinsicHeight());
                    icon.draw(c);
                }
            }
            super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
        }
    }

    private void onRowClick(TaskEntry entry) {
        if (MultiClickGuard.allowClick(getClass().getName())) {
            if (entry.type.equals("task")) {
                if (entry.locationTrigger != null && entry.locationTrigger.length() > 0) {
                    Toast.makeText(
                            getActivity(),
                            getString(R.string.smap_must_start_from_nfc),
                            Toast.LENGTH_LONG).show();
                } else {
                    ((SmapMain) getActivity()).completeTask(entry, false);
                }
            } else {
                ((SmapMain) getActivity()).completeForm(entry, false, null);
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        sortingOptions = new int[]{
                org.odk.collect.strings.R.string.sort_by_name_asc, org.odk.collect.strings.R.string.sort_by_name_desc,
                org.odk.collect.strings.R.string.sort_by_date_asc, org.odk.collect.strings.R.string.sort_by_date_desc,
                org.odk.collect.strings.R.string.sort_by_status_asc, org.odk.collect.strings.R.string.sort_by_status_desc,
                org.odk.collect.strings.R.string.sort_by_distance_asc, org.odk.collect.strings.R.string.sort_by_distance_desc
        };

        // Set up pull-to-refresh
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            ((SmapMain) getActivity()).processGetTask(true);
        });

        // Set up the Tasks / References toggle (only shown when references exist)
        filterGroup = view.findViewById(R.id.task_filter_group);
        btnTasks = view.findViewById(R.id.btn_tasks);
        btnReferences = view.findViewById(R.id.btn_references);
        filterGroup.check(R.id.btn_tasks);
        filterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            showReferences = (checkedId == R.id.btn_references);
            applyData();
        });

        model = getViewMode();
        model.getSurveyData().observe(getViewLifecycleOwner(), surveyData -> {
            Timber.i("-------------------------------------- Task List Fragment got Data ");
            setData(surveyData);
            // Stop the refresh animation when data is loaded
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        super.onViewCreated(view, savedInstanceState);

        // Notify the user if tracking is turned on
        Settings settings = DaggerUtils.getComponent(getContext()).settingsProvider().getUnprotectedSettings();
        if (new LocationRegister().locationEnabled() && settings.getBoolean(ProjectKeys.KEY_SMAP_USER_LOCATION)) {
            SnackbarUtils.showSnackbar(getActivity().findViewById(R.id.llParent), getString(R.string.smap_location_tracking), SnackbarUtils.DURATION_LONG);
        }
    }

    @Override
    public void onDestroyView() {
        rootView = null;
        super.onDestroyView();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle bundle) {
        super.onViewStateRestored(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_nav);

        if (bottomSheetDialog == null) {
            setupBottomSheet();
        }

    }

    private void setupBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(getActivity(), new ThemeUtils(getContext()).getBottomDialogTheme());
        View sheetView = getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet, null);
        final RecyclerView recyclerView = sheetView.findViewById(R.id.recyclerView);

        final SortDialogAdapter adapter = new SortDialogAdapter(getActivity(), sortingOptions, model.getTaskSortingOrder(),
                (itAdapter, position) -> {
                    model.saveTaskSelectedSortingOrder(position);
                    itAdapter.updateSelectedPosition(position);
                    reloadData();
                    bottomSheetDialog.dismiss();
                }, new SystemLocationProvider(getActivity()));
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        bottomSheetDialog.setContentView(sheetView);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
    }

    public void setData(SurveyData data) {
        allTasks = (data != null) ? data.tasks : null;
        applyData();
    }

    /*
     * Apply the current Tasks / References filter to the stored task list, update the toggle
     * visibility / labels and set the Tasks tab count to the number of actionable items only.
     */
    private void applyData() {
        if (mAdapter == null) {
            return;
        }

        // Count actionable tasks/cases and read only references
        int actionable = 0;
        int references = 0;
        if (allTasks != null) {
            for (TaskEntry entry : allTasks) {
                if ("form".equals(entry.type)) {
                    continue;
                }
                if (entry.taskType != null && entry.taskType.equals("reference")) {
                    references++;
                } else {
                    actionable++;
                }
            }
        }

        // The toggle is only useful when there are references to switch to
        if (filterGroup != null) {
            filterGroup.setVisibility(references > 0 ? View.VISIBLE : View.GONE);
            if (references == 0 && showReferences) {
                showReferences = false;
                filterGroup.check(R.id.btn_tasks);
            }
            if (btnTasks != null) {
                btnTasks.setText(getString(R.string.smap_tasks) + " (" + actionable + ")");
            }
            if (btnReferences != null) {
                btnReferences.setText(getString(R.string.smap_references) + " (" + references + ")");
            }
        }

        mAdapter.setShowReferences(showReferences);
        mAdapter.setData(allTasks);

        if (emptyView != null) {
            emptyView.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }

        // The tab badge always reflects actionable work, never references
        FragmentActivity activity = (SmapMain) getActivity();
        if (activity != null) {
            TabLayout tabLayout = (TabLayout) (activity).findViewById(R.id.tabs);
            if (tabLayout != null) {
                TabLayout.Tab tab = tabLayout.getTabAt(1);
                if (tab != null) {
                    tab.setText(getString(R.string.smap_tasks) + "(" + actionable + ")");
                }
            }
        }
    }

    public void setRefreshing(boolean refreshing) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    public SurveyDataViewModel getViewMode() {
        return ((SmapMain) getActivity()).getViewModel();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);

        menu.clear(); // smap - prevent duplicates from multiple fragments
        getActivity().getMenuInflater().inflate(R.menu.smap_menu, menu);

        Settings settings = DaggerUtils.getComponent(getContext()).settingsProvider().getUnprotectedSettings();
        boolean odkMenus = settings.getBoolean(ProjectKeys.KEY_SMAP_ODK_STYLE_MENUS);

        if (odkMenus) {
            menu
                    .add(0, MENU_ENTERDATA, 0, org.odk.collect.strings.R.string.enter_data)
                    .setIcon(android.R.drawable.ic_menu_edit)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            menu
                    .add(0, MENU_GETFORMS, 0, org.odk.collect.strings.R.string.get_forms)
                    .setIcon(android.R.drawable.ic_input_add)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            menu
                    .add(0, MENU_SENDDATA, 0, org.odk.collect.strings.R.string.send_data)
                    .setIcon(android.R.drawable.ic_menu_send)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            menu
                    .add(0, MENU_MANAGEFILES, 0, org.odk.collect.strings.R.string.manage_files)
                    .setIcon(android.R.drawable.ic_delete)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        menu
                .add(0, MENU_HISTORY, 0, R.string.smap_history)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        menu
                .add(0, MENU_EXIT, 0, org.odk.collect.strings.R.string.exit)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // smap - conditionally show admin menu
        boolean adminMenu = settings.getBoolean(ProjectKeys.KEY_SMAP_ODK_ADMIN_MENU);
        MenuItem adminItem = menu.findItem(R.id.menu_admin_preferences);
        if (adminItem != null) {
            adminItem.setVisible(adminMenu);
        }

        final MenuItem sortItem = menu.findItem(R.id.menu_sort);
        final MenuItem searchItem = menu.findItem(R.id.menu_filter);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getResources().getString(org.odk.collect.strings.R.string.search));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        if (filterText == null) {
            filterText = "";
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterText = query;
                reloadData();
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!filterText.equals(newText)) {
                    filterText = newText;
                    reloadData();
                }
                return false;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                sortItem.setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                sortItem.setVisible(true);
                return true;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_about) {
            startActivity(new Intent(getActivity(), AboutActivity.class));
            return true;
        } else if (itemId == R.id.menu_general_preferences) {
            startActivity(new Intent(getActivity(), ProjectPreferencesActivity.class));
            return true;
        } else if (itemId == R.id.menu_gettasks) {
            ((SmapMain) getActivity()).processGetTask(true);
            return true;
        } else if (itemId == R.id.menu_admin_preferences) {  // smap admin menu
            Settings adminSettings = DaggerUtils.getComponent(getContext()).settingsProvider().getProtectedSettings();
            String pw = adminSettings.getString(ProtectedProjectKeys.KEY_ADMIN_PW);
            if (pw == null || pw.isEmpty()) {
                Intent i = new Intent(getActivity(), AdminPreferencesActivitySmap.class);
                startActivity(i);
            } else {
                ((SmapMain) getActivity()).processAdminMenu();
            }
            return true;
        } else if (itemId == R.id.menu_sort) {
            if (bottomSheetDialog != null) {
                bottomSheetDialog.show();
            }
            return true;
        }

        switch (itemId) {
            case MENU_ENTERDATA:
                processEnterData();
                return true;
            case MENU_GETFORMS:
                processGetForms();
                return true;
            case MENU_SENDDATA:
                processSendData();
                return true;
            case MENU_MANAGEFILES:
                processManageFiles();
                return true;
            case MENU_HISTORY:
                ((SmapMain) getActivity()).processHistory();
                return true;
            case MENU_EXIT:
                ((SmapMain) getActivity()).exit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected CharSequence getFilterText() {
        return filterText != null ? filterText : "";
    }

    protected void reloadData() {
        if (model != null) {
            model.updateFilter(getFilterText());
            model.loadData();
        }
    }

    private void processEnterData() {
        // smap - use BlankFormListActivity (replaces old FillBlankFormActivity)
        if (MultiClickGuard.allowClick(getClass().getName())) {
            Intent i = new Intent(getContext(),
                    org.odk.collect.android.formlists.blankformlist.BlankFormListActivity.class);
            startActivity(i);
        }
    }

    // Get new forms
    private void processGetForms() {

        Intent i = new Intent(getContext(), FormDownloadListActivity.class);
        startActivity(i);
    }

    // Send data
    private void processSendData() {
        Intent i = new Intent(getContext(), org.odk.collect.android.instancemanagement.send.InstanceUploaderListActivity.class);
        startActivity(i);
    }

    private void processManageFiles() {
        Intent i = new Intent(getContext(), org.odk.collect.android.activities.DeleteFormsActivity.class);
        startActivity(i);
    }

    private void rejectTask(String reason, TaskEntry taskEntry) {
        if (Utilities.canReject(taskEntry.taskStatus)) {
            if (!taskEntry.taskStatus.equals("new") && reason != null && reason.trim().length() < 5) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setMessage(getString(R.string.smap_reason_not_specified))
                        .show();
            } else {
                Utilities.setStatusForTask(taskEntry.id, Utilities.STATUS_T_REJECTED, reason);
                Intent intent = new Intent("org.smap.smapTask.refresh");      // Notify map and task list of change
                LocalBroadcastManager.getInstance(requireActivity().getApplication()).sendBroadcast(intent);
                Timber.i("######## send org.smap.smapTask.refresh from taskAddressActivity");
            }
        } else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getString(R.string.smap_cannot_reject))
                    .show();
        }
    }


}
