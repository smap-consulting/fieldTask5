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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.AboutActivity;
import au.smap.fieldTask.activities.SmapMain;
import au.smap.fieldTask.viewmodels.SurveyDataViewModel;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.injection.DaggerUtils;
import au.smap.fieldTask.loaders.MapLocationObserver;
import au.smap.fieldTask.loaders.PointEntry;
import au.smap.fieldTask.loaders.SurveyData;
import au.smap.fieldTask.loaders.TaskEntry;
import org.odk.collect.androidshared.ui.FragmentFactoryBuilder;
import org.odk.collect.maps.LineDescription;
import org.odk.collect.maps.MapFragment;
import org.odk.collect.maps.MapFragmentFactory;
import org.odk.collect.maps.MapPoint;
import org.odk.collect.maps.markers.MarkerDescription;
import org.odk.collect.maps.markers.MarkerIconDescription;
import org.odk.collect.settings.keys.ProtectedProjectKeys;
import org.odk.collect.settings.keys.ProjectKeys;  // smap admin menu
import org.odk.collect.shared.settings.Settings;  // smap admin menu
import au.smap.fieldTask.preferences.AdminPreferencesActivitySmap;
import org.odk.collect.android.preferences.screens.ProjectPreferencesActivity;
import au.smap.fieldTask.utilities.KeyValueJsonFns;
import au.smap.fieldTask.utilities.Utilities;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Responsible for displaying tasks on the main fieldTask screen
 */
public class SmapTaskMapFragment extends Fragment {

    View rootView;

    private MapLocationObserver mo = null;
    private MapFragment mapFragment;
    private int polyFeatureId = -1;
    private Map<Integer, TaskEntry> markerTaskMap = new HashMap<>();

    SurveyDataViewModel model;

    @Inject
    MapFragmentFactory mapFragmentFactory;

    public static SmapTaskMapFragment newInstance() {
        return new SmapTaskMapFragment();
    }

    public SmapTaskMapFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        DaggerUtils.getComponent(context).inject(this);
        getChildFragmentManager().setFragmentFactory(
            new FragmentFactoryBuilder()
                .forClass(MapFragment.class, () -> (Fragment) mapFragmentFactory.createMapFragment())
                .build()
        );
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.ft_map_layout, container, false);
        setHasOptionsMenu(true);
        return rootView;
    }

    public SurveyDataViewModel getViewMode() {
        return ((SmapMain) getActivity()).getViewModel();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model = getViewMode();
        model.getSurveyData().observe(getViewLifecycleOwner(), surveyData -> {
            Timber.i("-------------------------------------- Task Map Fragment got Data ");
            setData(surveyData);
        });

        Fragment fragment = ((FragmentContainerView) view.findViewById(R.id.map_container)).getFragment();
        ((MapFragment) fragment).init(this::initMap, () -> {});
    }

    private void initMap(MapFragment map) {
        this.mapFragment = map;
        mapFragment.setLongPressListener(this::onMapLongPress);
        mapFragment.setFeatureClickListener(this::onFeatureClick);

        if (mo == null) {
            mo = new MapLocationObserver(getContext(), this);
        }

        // Refresh the data
        Intent intent = new Intent("org.smap.smapTask.refresh");
        LocalBroadcastManager.getInstance(Collect.getInstance()).sendBroadcast(intent);
        Timber.i("######## send org.smap.smapTask.refresh from smapTaskMapFragment");
    }

    @Override
    public void onDestroyView() {
        rootView = null;
        super.onDestroyView();
    }

    public void permissionsGranted() {
        // No-op: map abstraction handles location internally
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear(); // smap - prevent duplicates from multiple fragments
        getActivity().getMenuInflater().inflate(R.menu.smap_menu_map, menu);
        super.onCreateOptionsMenu(menu, inflater);

        // smap - conditionally show admin menu
        Settings settings = DaggerUtils.getComponent(getContext()).settingsProvider().getUnprotectedSettings();
        boolean adminMenu = settings.getBoolean(ProjectKeys.KEY_SMAP_ODK_ADMIN_MENU);
        MenuItem adminItem = menu.findItem(R.id.menu_admin_preferences);
        if (adminItem != null) {
            adminItem.setVisible(adminMenu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        if (itemId == R.id.menu_about) {
            Intent aboutIntent = new Intent(getActivity(), AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        } else if (itemId == R.id.menu_general_preferences) {
            Intent ig = new Intent(getActivity(), ProjectPreferencesActivity.class);
            startActivity(ig);
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
        } else if (itemId == R.id.menu_history) {
            ((SmapMain) getActivity()).processHistory();
            return true;
        } else if (itemId == R.id.menu_exit) {
            ((SmapMain) getActivity()).exit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void setData(SurveyData data) {
        if (data != null) {
            showTasks(data.tasks);
            showPoints(data.points);
        } else {
            clearTasks();
        }
    }

    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_nav);
        model.loadData();   // Update the user trail display with latest points
        super.onResume();
    }

    private void clearTasks() {
        if (mapFragment == null) return;
        for (int featureId : markerTaskMap.keySet()) {
            mapFragment.removeFeature(featureId);
        }
        markerTaskMap.clear();
    }

    private void showTasks(List<TaskEntry> data) {
        if (mapFragment == null) return;
        clearTasks();

        for (TaskEntry t : data) {
            if (t.type.equals("task")) {
                MapPoint point = getTaskMapPoint(t);
                if (point != null) {
                    int iconDrawable = getIconDrawable(t.taskStatus, t.repeat, t.locationTrigger != null, t.taskFinish);
                    MarkerDescription desc = new MarkerDescription(
                        point, false, MapFragment.BOTTOM,
                        new MarkerIconDescription.DrawableResource(iconDrawable)
                    );
                    int featureId = mapFragment.addMarker(desc);
                    markerTaskMap.put(featureId, t);
                }
            }
        }
    }

    private void showPoints(List<PointEntry> data) {
        if (mapFragment == null) return;
        if (polyFeatureId != -1) {
            mapFragment.removeFeature(polyFeatureId);
            polyFeatureId = -1;
        }
        List<MapPoint> points = new ArrayList<>();
        // Add in reverse order
        for (int i = data.size() - 1; i >= 0; i--) {
            points.add(new MapPoint(data.get(i).lat, data.get(i).lon));
        }
        polyFeatureId = mapFragment.addPolyLine(new LineDescription(points, null, null, false, false));
    }

    public void updatePath(MapPoint point) {
        if (mapFragment != null && polyFeatureId != -1) {
            mapFragment.appendPointToPolyLine(polyFeatureId, point);
        }
    }

    /*
     * Get the coordinates of the task
     */
    private MapPoint getTaskMapPoint(TaskEntry t) {
        double lat = (t.actLat == 0.0 && t.actLon == 0.0) ? t.schedLat : t.actLat;
        double lon = (t.actLat == 0.0 && t.actLon == 0.0) ? t.schedLon : t.actLon;
        return (lat != 0.0 || lon != 0.0) ? new MapPoint(lat, lon) : null;
    }

    /*
     * Get the drawable resource to represent the passed in task status
     */
    private int getIconDrawable(String status, boolean isRepeat, boolean hasTrigger, long taskFinish) {
        if (status.equals(Utilities.STATUS_T_REJECTED) || status.equals(Utilities.STATUS_T_CANCELLED)) {
            return R.drawable.form_state_rejected;
        } else if (status.equals(Utilities.STATUS_T_ACCEPTED)) {
            if (hasTrigger) {
                return R.drawable.form_state_triggered;
            } else if (isRepeat) {
                return R.drawable.form_state_repeat;
            } else if (taskFinish != 0 && taskFinish < (new Date()).getTime()) {
                return R.drawable.form_state_late;
            } else {
                return R.drawable.form_state_saved_circle;
            }
        } else if (status.equals(Utilities.STATUS_T_COMPLETE)) {
            return R.drawable.form_state_finalized_circle;
        } else if (status.equals(Utilities.STATUS_T_SUBMITTED)) {
            return R.drawable.form_state_submitted_circle;
        } else if (status.equals(Utilities.STATUS_T_NEW)) {
            return R.drawable.form_state_new;
        } else {
            Timber.i("Unknown task status: %s", status);
            return R.drawable.form_state_saved_circle;
        }
    }

    private void onMapLongPress(MapPoint point) {
        double minDistance = 1000;
        TaskEntry nearest = null;
        for (Map.Entry<Integer, TaskEntry> entry : markerTaskMap.entrySet()) {
            TaskEntry t = entry.getValue();
            MapPoint tp = getTaskMapPoint(t);
            if (tp != null) {
                double d = Math.sqrt(
                    Math.pow(tp.latitude - point.latitude, 2) +
                    Math.pow(tp.longitude - point.longitude, 2)
                );
                if (d < minDistance) {
                    minDistance = d;
                    nearest = t;
                }
            }
        }
        if (nearest != null) {
            Toast.makeText(getActivity(), "marker selected: " + nearest.name, Toast.LENGTH_LONG).show();
            if (nearest.locationTrigger != null) {
                Toast.makeText(
                    getActivity(),
                    getString(R.string.smap_must_start_from_nfc),
                    Toast.LENGTH_LONG).show();
            } else {
                ((SmapMain) getActivity()).completeTask(nearest, false);
            }
        }
    }

    private void onFeatureClick(int featureId) {
        TaskEntry t = markerTaskMap.get(featureId);
        if (t != null) {
            String taskTime = Utilities.getTaskTime(t.taskStatus, t.actFinish, t.taskStart);
            String addressText = KeyValueJsonFns.getValues(t.taskAddress);
            Toast.makeText(getActivity(), t.name + "\n" + taskTime + "\n" + addressText, Toast.LENGTH_SHORT).show();
        }
    }

    public void locateTask(TaskEntry task) {
        if (mapFragment != null) {
            mapFragment.zoomToPoint(new MapPoint(task.schedLat, task.schedLon), 16.0, true);
        }
    }
}
