/*
 * Copyright (C) 2018 Nafundi
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

package org.odk.collect.geo.geocompound;

import static org.odk.collect.geo.Constants.EXTRA_READ_ONLY;
import static org.odk.collect.geo.Constants.EXTRA_RETAIN_MOCK_ACCURACY;
import static org.odk.collect.geo.GeoActivityUtils.requireLocationPermissions;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.odk.collect.androidshared.ui.DialogFragmentUtils;
import org.odk.collect.androidshared.ui.FragmentFactoryBuilder;
import org.odk.collect.androidshared.ui.ToastUtils;
import org.odk.collect.async.Scheduler;
import org.odk.collect.externalapp.ExternalAppUtils;
import org.odk.collect.geo.Constants;
import org.odk.collect.geo.GeoDependencyComponentProvider;
import org.odk.collect.geo.GeoUtils;
import org.odk.collect.geo.R;
import org.odk.collect.geo.geopoint.AccuracyStatusView;
import org.odk.collect.geo.geopoint.LocationAccuracy;
import org.odk.collect.geo.geopoly.GeoPolySettingsDialogFragment;
import org.odk.collect.location.Location;
import org.odk.collect.location.tracker.LocationTracker;
import org.odk.collect.location.tracker.LocationTrackerKt;
import org.odk.collect.maps.LineDescription;
import org.odk.collect.maps.MapConsts;
import org.odk.collect.maps.MapFragmentFactory;
import org.odk.collect.maps.MapFragment;
import org.odk.collect.maps.MapPoint;
import org.odk.collect.maps.layers.OfflineMapLayersPickerBottomSheetDialogFragment;
import org.odk.collect.maps.layers.ReferenceLayerRepository;
import org.odk.collect.maps.markers.MarkerDescription;
import org.odk.collect.maps.markers.MarkerIconDescription;
import org.odk.collect.settings.SettingsProvider;
import org.odk.collect.strings.localization.LocalizedActivity;
import org.odk.collect.webpage.WebPageService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

public class GeoCompoundActivity extends LocalizedActivity implements GeoPolySettingsDialogFragment.SettingsDialogCallback,
        CompoundDialogFragment.SettingsDialogCallback {
    public static final String EXTRA_POLYGON = "answer";
    public static final String EXTRA_APPEARANCE = "appearances";
    public static final String POINTS_KEY = "points";
    public static final String MARKERS_KEY = "markers";
    public static final String INPUT_ACTIVE_KEY = "input_active";
    public static final String RECORDING_ENABLED_KEY = "recording_enabled";
    public static final String RECORDING_AUTOMATIC_KEY = "recording_automatic";
    public static final String INTERVAL_INDEX_KEY = "interval_index";
    public static final String ACCURACY_THRESHOLD_INDEX_KEY = "accuracy_threshold_index";
    protected Bundle previousState;

    private final ScheduledExecutorService executorServiceScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture schedulerHandler;

    @Inject
    MapFragmentFactory mapFragmentFactory;

    @Inject
    LocationTracker locationTracker;

    @Inject
    ReferenceLayerRepository referenceLayerRepository;

    @Inject
    Scheduler scheduler;

    @Inject
    SettingsProvider settingsProvider;

    @Inject
    WebPageService webPageService;

    private HashMap<Integer, CompoundMarker> markers = new HashMap<>();
    private HashMap<Integer, Integer> markerFeatureIds = new HashMap<>(); // vertex index -> marker featureId
    private HashMap<String, MarkerType> markerTypes = new HashMap<>();
    private MapFragment map;
    private int lineFeatureId = -1;  // the polyline feature ID
    private List<MapPoint> originalPoly;
    private String originalAppearanceString = "";

    private ImageButton zoomButton;
    ImageButton playButton;
    ImageButton clearButton;
    private Button recordButton;
    private ImageButton pauseButton;
    ImageButton backspaceButton;
    ImageButton saveButton;

    private AccuracyStatusView locationStatus;
    private TextView collectionStatus;

    private View settingsView;

    private static final int[] INTERVAL_OPTIONS = {
        1, 5, 10, 20, 30, 60, 300, 600, 1200, 1800
    };
    private static final int DEFAULT_INTERVAL_INDEX = 3; // default is 20 seconds

    private static final int[] ACCURACY_THRESHOLD_OPTIONS = {
        0, 3, 5, 10, 15, 20
    };
    private static final int DEFAULT_ACCURACY_THRESHOLD_INDEX = 3; // default is 10 meters

    private boolean inputActive; // whether we are ready for the user to add points
    private boolean recordingEnabled; // whether points are taken from GPS readings (if not, placed by tapping)
    private boolean recordingAutomatic; // whether GPS readings are taken at regular intervals (if not, only when user-directed)
    private boolean intentReadOnly; // whether the intent requested for the path to be read-only.

    private int intervalIndex = DEFAULT_INTERVAL_INDEX;

    private int accuracyThresholdIndex = DEFAULT_ACCURACY_THRESHOLD_INDEX;

    // restored from savedInstanceState
    private List<MapPoint> restoredPoints;
    private List<CompoundMarker> restoredMarkers;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (!intentReadOnly && map != null && !originalPoly.equals(map.getPolyPoints(lineFeatureId))) {
                showBackDialog();
            } else {
                finish();
            }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        ((GeoDependencyComponentProvider) getApplication()).getGeoDependencyComponent().inject(this);

        getSupportFragmentManager().setFragmentFactory(new FragmentFactoryBuilder()
                .forClass(MapFragment.class, () -> (Fragment) mapFragmentFactory.createMapFragment())
                .forClass(OfflineMapLayersPickerBottomSheetDialogFragment.class, () -> new OfflineMapLayersPickerBottomSheetDialogFragment(getActivityResultRegistry(), referenceLayerRepository, scheduler, settingsProvider, webPageService))
                .forClass(GeoPolySettingsDialogFragment.class, () -> new GeoPolySettingsDialogFragment(this))
                .build()
        );

        super.onCreate(savedInstanceState);

        requireLocationPermissions(this);

        previousState = savedInstanceState;

        if (savedInstanceState != null) {
            restoredPoints = savedInstanceState.getParcelableArrayList(POINTS_KEY);
            restoredMarkers = savedInstanceState.getParcelableArrayList(MARKERS_KEY);
            inputActive = savedInstanceState.getBoolean(INPUT_ACTIVE_KEY, false);
            recordingEnabled = savedInstanceState.getBoolean(RECORDING_ENABLED_KEY, false);
            recordingAutomatic = savedInstanceState.getBoolean(RECORDING_AUTOMATIC_KEY, false);
            intervalIndex = savedInstanceState.getInt(INTERVAL_INDEX_KEY, DEFAULT_INTERVAL_INDEX);
            accuracyThresholdIndex = savedInstanceState.getInt(
                ACCURACY_THRESHOLD_INDEX_KEY, DEFAULT_ACCURACY_THRESHOLD_INDEX);
            originalAppearanceString = savedInstanceState.getString(EXTRA_APPEARANCE, "");
        }

        intentReadOnly = getIntent().getBooleanExtra(EXTRA_READ_ONLY, false);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTitle(getString(org.odk.collect.strings.R.string.geocompound_title));
        setContentView(R.layout.geopoly_layout);

        MapFragment mapFragment = ((FragmentContainerView) findViewById(R.id.map_container)).getFragment();
        mapFragment.init(this::initMap, this::finish);

        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (map == null) {
            // initMap() is called asynchronously, so map can be null if the activity
            // is stopped (e.g. by screen rotation) before initMap() gets to run.
            // In this case, preserve any provided instance state.
            if (previousState != null) {
                state.putAll(previousState);
            }
            return;
        }
        state.putParcelableArrayList(POINTS_KEY, new ArrayList<>(map.getPolyPoints(lineFeatureId)));
        state.putParcelableArrayList(MARKERS_KEY, new ArrayList<>(getMarkerArray()));
        state.putBoolean(INPUT_ACTIVE_KEY, inputActive);
        state.putBoolean(RECORDING_ENABLED_KEY, recordingEnabled);
        state.putBoolean(RECORDING_AUTOMATIC_KEY, recordingAutomatic);
        state.putInt(INTERVAL_INDEX_KEY, intervalIndex);
        state.putInt(ACCURACY_THRESHOLD_INDEX_KEY, accuracyThresholdIndex);
        state.putString(EXTRA_APPEARANCE, originalAppearanceString);
    }

    @Override protected void onDestroy() {
        if (schedulerHandler != null && !schedulerHandler.isCancelled()) {
            schedulerHandler.cancel(true);
        }

        locationTracker.stop();
        super.onDestroy();
    }

    public void initMap(MapFragment newMapFragment) {
        map = newMapFragment;

        locationStatus = findViewById(R.id.location_status);
        collectionStatus = findViewById(R.id.collection_status);
        settingsView = getLayoutInflater().inflate(R.layout.geopoly_dialog, null);

        clearButton = findViewById(R.id.clear);
        clearButton.setOnClickListener(v -> showClearDialog());

        pauseButton = findViewById(R.id.pause);
        pauseButton.setOnClickListener(v -> {
            inputActive = false;
            try {
                schedulerHandler.cancel(true);
            } catch (Exception e) {
                // Do nothing
            }
            updateUi();
        });

        backspaceButton = findViewById(R.id.backspace);
        backspaceButton.setOnClickListener(v -> removeLastPoint());

        saveButton = findViewById(R.id.save);
        saveButton.setOnClickListener(v -> {
            saveAsGeoCompound();
        });

        playButton = findViewById(R.id.play);
        playButton.setOnClickListener(v -> {
            if (map.getPolyPoints(lineFeatureId).isEmpty()) {
                DialogFragmentUtils.showIfNotShowing(GeoPolySettingsDialogFragment.class, getSupportFragmentManager());
            } else {
                startInput();
            }
        });

        recordButton = findViewById(R.id.record_button);
        recordButton.setOnClickListener(v -> recordPoint(map.getGpsLocation()));

        findViewById(R.id.layers).setOnClickListener(v -> {
            DialogFragmentUtils.showIfNotShowing(OfflineMapLayersPickerBottomSheetDialogFragment.class, getSupportFragmentManager());
        });

        zoomButton = findViewById(R.id.zoom);
        zoomButton.setOnClickListener(v -> map.zoomToPoint(map.getGpsLocation(), true));

        // Get the marker types from the appearance
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_APPEARANCE)) {
            originalAppearanceString = intent.getStringExtra(EXTRA_APPEARANCE);
            markerTypes = getMarkerTypes(originalAppearanceString);
        }
        if(originalAppearanceString != null) {
            markerTypes = getMarkerTypes(originalAppearanceString);
        }

        List<MapPoint> points = new ArrayList<>();
        if (intent != null && intent.hasExtra(EXTRA_POLYGON)) {
            String answer = intent.getStringExtra(EXTRA_POLYGON);
            GeoCompoundData cd = new GeoCompoundData(answer, markerTypes);
            points = cd.points;
            markers = cd.markers;
        }
        if (restoredPoints != null) {
            points = restoredPoints;
            markers = getMarkerHashMap(restoredMarkers);
        }

        originalPoly = points;

        if(map.getPolyPoints(lineFeatureId).size() > 0) {
            points = map.getPolyPoints(lineFeatureId);
        } else {
            // Create the polyline
            lineFeatureId = map.addPolyLine(new LineDescription(points, null, null, false, false)); // smap - draggable=false avoids TracePoint circles that obscure compound markers

            // Create markers for marked vertices
            createMarkersForVertices();
        }

        // Set up marker click listener
        map.setFeatureClickListener(this::onFeatureClicked);

        if (inputActive && !intentReadOnly) {
            startInput();
        }

        map.setClickListener(this::onClick);
        // Also allow long press to place point to match prior versions
        map.setLongPressListener(this::onClick);
        map.setGpsLocationEnabled(true);
        map.setGpsLocationListener(this::onGpsLocation);
        if (!points.isEmpty()) {
            map.zoomToBoundingBox(points, 0.6, false);
        } else {
            map.runOnGpsLocationReady(this::onGpsLocationReady);
        }
        updateUi();
    }

    private void createMarkersForVertices() {
        List<MapPoint> points = map.getPolyPoints(lineFeatureId);
        for (int i = 0; i < points.size(); i++) {
            createOrUpdateMarker(i);
        }
    }

    private void createOrUpdateMarker(int vertexIdx) {
        List<MapPoint> points = map.getPolyPoints(lineFeatureId);
        if (vertexIdx < 0 || vertexIdx >= points.size()) {
            return;
        }

        MapPoint point = points.get(vertexIdx);
        CompoundMarker cm = markers.get(vertexIdx);

        // smap - untyped vertices use TracePoint circles to match GeoTrace node appearance
        MarkerIconDescription iconDesc;
        if (cm != null && !cm.type.equals("none")) {
            iconDesc = new MarkerIconDescription.DrawableResource(cm.getDrawableIdForMarker());
        } else {
            iconDesc = new MarkerIconDescription.TracePoint(
                    MapConsts.DEFAULT_STROKE_WIDTH,
                    MapConsts.DEFAULT_STROKE_COLOR
            );
        }

        Integer existingFeatureId = markerFeatureIds.get(vertexIdx);
        if (existingFeatureId != null) {
            map.setMarkerIcon(existingFeatureId, iconDesc);
        } else {
            MarkerDescription markerDesc = new MarkerDescription(point, false, MapFragment.CENTER, iconDesc);
            int newFeatureId = map.addMarker(markerDesc);
            markerFeatureIds.put(vertexIdx, newFeatureId);
        }
    }

    private void saveAsGeoCompound() {
        String result = "";     // Default result if there are no points
        if (map.getPolyPoints(lineFeatureId).size() == 1) {
            ToastUtils.showShortToast(getString(org.odk.collect.strings.R.string.polyline_validator));
            return;     // do not finish
        } else if (map.getPolyPoints(lineFeatureId).size() > 1) {
            List<MapPoint> points = map.getPolyPoints(lineFeatureId);
            StringBuilder rb = new StringBuilder("line:")
                    .append(GeoUtils.formatPointsResultString(points, false))
                    .append(getMarkersAsText(points));
            result = rb.toString();
        }
        ExternalAppUtils.returnSingleValue(this, result);
    }

    @Override
    public void startInput() {
        inputActive = true;
        if (recordingEnabled && recordingAutomatic) {
            locationTracker.start();

            recordPoint(map.getGpsLocation());
            schedulerHandler = executorServiceScheduler.scheduleAtFixedRate(() -> runOnUiThread(() -> {
                Location currentLocation = LocationTrackerKt.getCurrentLocation(locationTracker);

                if (currentLocation != null) {
                    MapPoint currentMapPoint = new MapPoint(
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude(),
                            currentLocation.getAltitude(),
                            currentLocation.getAccuracy()
                    );

                    recordPoint(currentMapPoint);
                }
            }), 0, INTERVAL_OPTIONS[intervalIndex], TimeUnit.SECONDS);
        }
        updateUi();
    }

    @Override
    public void updateMarker(int markerId, String markerType) {
        if ("none".equals(markerType)) {
            markers.remove(markerId);
        } else {
            CompoundMarker cm = markers.get(markerId);
            if (cm == null) {
                cm = new CompoundMarker(markerId, markerType, getMarkerTypeLabel(markerType));
                markers.put(markerId, cm);
            } else {
                cm.type = markerType;
                cm.label = getMarkerTypeLabel(markerType);
            }
        }
        createOrUpdateMarker(markerId);
    }

    @Override
    public void updateRecordingMode(int id) {
        recordingEnabled = id != R.id.placement_mode;
        recordingAutomatic = id == R.id.automatic_mode;
    }

    @Override
    public int getCheckedId() {
        if (recordingEnabled) {
            return recordingAutomatic ? R.id.automatic_mode : R.id.manual_mode;
        } else {
            return R.id.placement_mode;
        }
    }

    @Override
    public int getIntervalIndex() {
        return intervalIndex;
    }

    @Override
    public int getAccuracyThresholdIndex() {
        return accuracyThresholdIndex;
    }

    @Override
    public void setIntervalIndex(int intervalIndex) {
        this.intervalIndex = intervalIndex;
    }

    @Override
    public void setAccuracyThresholdIndex(int accuracyThresholdIndex) {
        this.accuracyThresholdIndex = accuracyThresholdIndex;
    }

    /**
     * Reacts to a click on a feature (marker or polyline vertex) by showing a dialog to get the marker type
     */
    public void onFeatureClicked(int featureId) {
        if(inputActive) {
            return;
        }

        // Find which vertex this corresponds to
        Integer markerIdx = null;
        for (HashMap.Entry<Integer, Integer> entry : markerFeatureIds.entrySet()) {
            if (entry.getValue() == featureId) {
                markerIdx = entry.getKey();
                break;
            }
        }

        if (markerIdx == null && featureId == lineFeatureId) {
            // Clicked on polyline line segment - find nearest vertex
            markerIdx = findNearestVertex();
        }

        if (markerIdx != null) {
            showMarkerDialog(markerIdx);
        }
    }

    private Integer findNearestVertex() {
        MapPoint gps = map.getGpsLocation();
        MapPoint center = map.getCenter();
        MapPoint ref = (gps != null) ? gps : center;
        List<MapPoint> points = map.getPolyPoints(lineFeatureId);
        if (points.isEmpty()) {
            return null;
        }
        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < points.size(); i++) {
            MapPoint p = points.get(i);
            double dist = Math.pow(p.latitude - ref.latitude, 2) + Math.pow(p.longitude - ref.longitude, 2);
            if (dist < minDist) {
                minDist = dist;
                nearest = i;
            }
        }
        return nearest;
    }

    private void showMarkerDialog(int markerIdx) {
        Timber.i("Marker: %s", markerIdx);
        CompoundMarker marker = markers.get(markerIdx);
        DialogFragment df = new CompoundDialogFragment();
        Bundle args = new Bundle();
        args.putString(CompoundDialogFragment.PIT_KEY, getMarkerTypeName("pit"));
        args.putString(CompoundDialogFragment.FAULT_KEY, getMarkerTypeName("fault"));
        args.putInt(CompoundDialogFragment.FEATUREID_KEY, markerIdx);
        args.putString(CompoundDialogFragment.LABEL_KEY, getMarkerLabel(markerIdx));
        if (marker != null) {
            args.putString(CompoundDialogFragment.VALUE_KEY, marker.type);
        }
        df.setArguments(args);
        df.show(getSupportFragmentManager(), CompoundDialogFragment.class.getName());
    }

    private void onClick(MapPoint point) {
        if (inputActive && !recordingEnabled) {
            map.appendPointToPolyLine(lineFeatureId, point);
            int newIdx = map.getPolyPoints(lineFeatureId).size() - 1;
            createOrUpdateMarker(newIdx);
            updateUi();
        }
    }

    private void onGpsLocationReady(MapFragment map) {
        // Don't zoom to current location if a user is manually entering points
        if (getWindow().isActive() && (!inputActive || recordingEnabled)) {
            map.zoomToPoint(map.getGpsLocation(), true);
        }
        updateUi();
    }

    private void onGpsLocation(MapPoint point) {
        if (inputActive && recordingEnabled) {
            map.setCenter(point, false);
        }
        updateUi();
    }

    private void recordPoint(MapPoint point) {
        if (point != null && isLocationAcceptable(point)) {
            map.appendPointToPolyLine(lineFeatureId, point);
            int newIdx = map.getPolyPoints(lineFeatureId).size() - 1;
            createOrUpdateMarker(newIdx);
            updateUi();
        }
    }

    private boolean isLocationAcceptable(MapPoint point) {
        if (!isAccuracyThresholdActive()) {
            return true;
        }
        return point.accuracy <= ACCURACY_THRESHOLD_OPTIONS[accuracyThresholdIndex];
    }

    private boolean isAccuracyThresholdActive() {
        int meters = ACCURACY_THRESHOLD_OPTIONS[accuracyThresholdIndex];
        return recordingEnabled && recordingAutomatic && meters > 0;
    }

    private void removeLastPoint() {
        if (lineFeatureId != -1) {
            int numPoints = map.getPolyPoints(lineFeatureId).size();
            if (numPoints > 0) {
                int lastIdx = numPoints - 1;
                Integer markerFeatureId = markerFeatureIds.remove(lastIdx);
                if (markerFeatureId != null) {
                    map.removeFeature(markerFeatureId);
                }
                markers.remove(lastIdx);
                map.removePolyLineLastPoint(lineFeatureId);
            }
            updateUi();
        }
    }

    private void clear() {
        map.clearFeatures();
        lineFeatureId = map.addPolyLine(new LineDescription(new ArrayList<>(), null, null, false, false)); // smap
        markerFeatureIds.clear();
        markers.clear();
        inputActive = false;
        updateUi();
    }

    /** Updates the state of various UI widgets to reflect internal state. */
    private void updateUi() {
        final int numPoints = map.getPolyPoints(lineFeatureId).size();
        final MapPoint location = map.getGpsLocation();

        // Visibility state
        playButton.setVisibility(inputActive ? View.GONE : View.VISIBLE);
        pauseButton.setVisibility(inputActive ? View.VISIBLE : View.GONE);
        recordButton.setVisibility(inputActive && recordingEnabled && !recordingAutomatic ? View.VISIBLE : View.GONE);

        // Enabled state
        zoomButton.setEnabled(location != null);
        backspaceButton.setEnabled(numPoints > 0);
        clearButton.setEnabled(!inputActive && numPoints > 0);
        settingsView.findViewById(R.id.manual_mode).setEnabled(location != null);
        settingsView.findViewById(R.id.automatic_mode).setEnabled(location != null);

        if (intentReadOnly) {
            playButton.setEnabled(false);
            backspaceButton.setEnabled(false);
            clearButton.setEnabled(false);
        }

        // GPS status
        boolean usingThreshold = isAccuracyThresholdActive();
        boolean acceptable = location != null && isLocationAcceptable(location);
        int seconds = INTERVAL_OPTIONS[intervalIndex];
        int minutes = seconds / 60;
        int meters = ACCURACY_THRESHOLD_OPTIONS[accuracyThresholdIndex];

        if (location != null) {
            LocationAccuracy accuracy;
            if (!usingThreshold) {
                accuracy = new LocationAccuracy.Improving((float) location.accuracy);
            } else if (acceptable) {
                accuracy = new LocationAccuracy.Poor((float) location.accuracy);
            } else {
                accuracy = new LocationAccuracy.Unacceptable((float) location.accuracy);
            }
            locationStatus.setAccuracy(accuracy);
        } else {
            locationStatus.setAccuracy(null);
        }

        collectionStatus.setText(
            !inputActive ? getString(org.odk.collect.strings.R.string.collection_status_paused, numPoints)
                : !recordingEnabled ? getString(org.odk.collect.strings.R.string.collection_status_placement, numPoints)
                : !recordingAutomatic ? getString(org.odk.collect.strings.R.string.collection_status_manual, numPoints)
                : !usingThreshold ? (
                    minutes > 0 ?
                        getString(org.odk.collect.strings.R.string.collection_status_auto_minutes, numPoints, minutes) :
                        getString(org.odk.collect.strings.R.string.collection_status_auto_seconds, numPoints, seconds)
                )
                : (
                    minutes > 0 ?
                        getString(org.odk.collect.strings.R.string.collection_status_auto_minutes_accuracy, numPoints, minutes, meters) :
                        getString(org.odk.collect.strings.R.string.collection_status_auto_seconds_accuracy, numPoints, seconds, meters)
                )
        );
    }

    private void showClearDialog() {
        if (!map.getPolyPoints(lineFeatureId).isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                .setMessage(org.odk.collect.strings.R.string.geo_clear_warning)
                .setPositiveButton(org.odk.collect.strings.R.string.clear, (dialog, id) -> clear())
                .setNegativeButton(org.odk.collect.strings.R.string.cancel, null)
                .show();
        }
    }

    private void showBackDialog() {
        new MaterialAlertDialogBuilder(this)
            .setMessage(getString(org.odk.collect.strings.R.string.geo_exit_warning, getString(org.odk.collect.strings.R.string.app_name))) // smap
            .setPositiveButton(org.odk.collect.strings.R.string.discard, (dialog, id) -> finish())
            .setNegativeButton(org.odk.collect.strings.R.string.cancel, null)
            .show();
    }

    @VisibleForTesting public MapFragment getMapFragment() {
        return map;
    }

    private String getMarkersAsText(List<MapPoint> points) {
        StringBuilder out = new StringBuilder("");

        Collection<Integer> indexes = markers.keySet();
        List<Integer> list = new ArrayList<>(indexes);
        java.util.Collections.sort(list);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        for(int index : list) {
            if (index >= points.size()) {
                continue;
            }
            MapPoint point = points.get(index);
            CompoundMarker cm = markers.get(index);

            String location = String.format(Locale.US, "%s %s %s %s;",
                    point.latitude, point.longitude,
                    point.altitude, (float) point.accuracy);

            out.append("#marker:")
                    .append(location)
                    .append(":index=")
                    .append(index)
                    .append(";type=")
                    .append(cm.type);
            try {
                String address = getAddress(geocoder, point.latitude, point.longitude);
                // Note: fieldTask5 doesn't have Collect.getInstance(), so address storage removed
                // Address functionality may need to be reimplemented if needed
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        return out.toString();
    }

    private String getAddress(Geocoder geocoder, Double lat, Double lon) throws Exception{
        StringBuilder sAddress = new StringBuilder("");
        List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

        if (addresses != null && !addresses.isEmpty()) {
            Address a1 = addresses.get(0);

            for (int i = 0; i <= a1.getMaxAddressLineIndex(); i++) {
                if(i > 0) {
                    sAddress.append(", ");
                }
                sAddress.append(a1.getAddressLine(i));
            }
        }
        return sAddress.toString();
    }

    private String getMarkerTypeName(String type) {
        String name = type;
        MarkerType mt = markerTypes.get(type);
        if(mt != null) {
            name = mt.name;
        }
        return name;
    }

    private String getMarkerTypeLabel(String type) {
        String label = "";
        MarkerType mt = markerTypes.get(type);
        if(mt != null) {
            label = mt.label;
        }
        return label;
    }

    /*
     *  Append an index number to the label
     *  If marker is the third one encountered of the same type starting from the first marker
     *   then append "3" to the label and so on.
     */
    private String getMarkerLabel(int markerIdx) {
        String label = "";
        CompoundMarker marker = markers.get(markerIdx);

        if(marker != null) {
            Collection<Integer> indexes = markers.keySet();
            List<Integer> list = new ArrayList<>(indexes);
            java.util.Collections.sort(list);

            int labelIndex = 1;
            for (int index : list) {
                if (index == markerIdx) {
                    return marker.label + labelIndex;
                }
                CompoundMarker cm = markers.get(index);
                if (cm != null && cm.type.equals(marker.type)) {
                    labelIndex++;
                }

            }
        }
        return label;
    }

    private ArrayList<CompoundMarker> getMarkerArray() {
        return new ArrayList(markers.values());
    }

    private HashMap<Integer, CompoundMarker> getMarkerHashMap(List<CompoundMarker> markerArray) {
        HashMap<Integer, CompoundMarker> markers = new HashMap<>();
        if(markerArray != null) {
            for(CompoundMarker cm : markerArray) {
                markers.put(cm.index, cm);
            }
        }
        return markers;
    }

    private HashMap<String, MarkerType> getMarkerTypes(String appearance) {
        HashMap<String, MarkerType> markerTypes = new HashMap<>();
        if(appearance != null) {
            String components[] = appearance.split(" ");
            for (int i = 0; i < components.length; i++) {
                if(components[i].startsWith("marker:")) {
                    String componentParts [] = components[i].split(":");
                    if(componentParts.length >= 4) {
                        String type = componentParts[1];
                        String name = componentParts[2];
                        String label = componentParts[3];
                        markerTypes.put(type, new MarkerType(type, name, label));
                    }
                }
            }
        }
        return markerTypes;
    }
}
