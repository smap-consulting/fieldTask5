package au.smap.fieldTask.loaders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;

import org.odk.collect.android.application.Collect;
import au.smap.fieldTask.fragments.SmapTaskMapFragment;
import org.odk.collect.maps.MapPoint;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class MapLocationObserver extends BroadcastReceiver {

    private SmapTaskMapFragment mMap = null;

public MapLocationObserver(Context context, SmapTaskMapFragment map) {
    mMap = map;

    LocalBroadcastManager.getInstance(context).registerReceiver(this,
            new IntentFilter("locationChanged"));
  }

  @Override
  public void onReceive(Context context, Intent intent) {
      Location locn = Collect.getInstance().getLocation();
      MapPoint point = new MapPoint(locn.getLatitude(), locn.getLongitude());
      mMap.updatePath(point);
  }
}
