package zachcotter.madcourse.neu.edu.sensordatagrabber;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class DataCollectionService extends Service implements ConnectionCallbacks,
                                                              OnConnectionFailedListener,
                                                              LocationListener,
                                                              SensorEventListener {
  public static final String FILE_NAME = "datafile.csv";
  public static final String DATA_KEY = "data";
  private boolean connected;
  private LocationRequest locationRequest;
  private LocationClient locationClient;
  private static final long UPDATE_INTERVAL = 2000;

  private float distance;
  private Double lastLat;
  private Double lastLong;

  private SensorManager sensorManager;
  private Sensor accelorometer;

  private FileOutputStream fos;
  private int aCounter;

  private float lastX;
  private float lastY;
  private float lastZ;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    distance = 0;
    lastLat = null;
    lastLong = null;
    lastX = 0;
    lastY = 0;
    lastZ = 0;
    try {
      fos = openFileOutput(FILE_NAME, MODE_WORLD_READABLE);
    }
    catch(FileNotFoundException e) {
      e.printStackTrace();
    }
    aCounter = 0;
    locationRequest = LocationRequest.create();
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    locationRequest.setInterval(UPDATE_INTERVAL);
    locationRequest.setFastestInterval(UPDATE_INTERVAL);
    locationClient = new LocationClient(this,
                                        this,
                                        this);
    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    accelorometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
  }

  @Override
  public int onStartCommand(Intent intent,
                            int flags,
                            int startId) {
    locationClient.connect();
    sensorManager.registerListener(this,
                                   accelorometer,
                                   2000000);
    return super.onStartCommand(intent,
                                flags,
                                startId);
  }

  @Override
  public void onDestroy() {
    try {
      fos.close();
    }
    catch(IOException e) {
      e.printStackTrace();
    }
    if(connected) {
      locationClient.disconnect();
    }
    try {
      sensorManager.unregisterListener(this);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    super.onDestroy();
  }

  @Override
  public void onConnected(Bundle bundle) {
    connected = true;
    locationClient.requestLocationUpdates(locationRequest,
                                          this);
    Log.w("On",
          "Connected");
  }

  @Override
  public void onDisconnected() {
    connected = false;
    Log.w("On",
          "Disconnected");
  }

  private boolean isConnected() {
    return connected;
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    connected = false;
    Log.w("On",
          "Failed");
    // TODO show an error dialog
  }

  @Override
  public void onLocationChanged(Location location) {
    Log.w("location",
          "change");
    double latitude = location.getLatitude();
    double longitude = location.getLongitude();
    if(lastLat != null) {
      Location last = new Location("last");//this is the dumbest constructor i've ever seen
      last.setLatitude(lastLat);
      last.setLongitude(lastLong);
      Location current = new Location("current");
      current.setLatitude(latitude);
      current.setLongitude(longitude);
      double change = last.distanceTo(current);
      distance += change;
      storeDistance();
    }
    lastLat = latitude;
    lastLong = longitude;
  }

  public static final String DISTANCE_KEY = "dist";


  private void storeDistance() {
    //time lat long dist
    String row = "L," + System.currentTimeMillis() + "," +
      lastLat + "," +
      lastLong + "," +
      distance + "\n";
    Log.w("write",
          row);
    writeRow(row);
    SharedPreferences prefs = getSharedPreferences(DATA_KEY,
                                                   MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.putFloat(DISTANCE_KEY,
                    distance);
    editor.commit();
  }

  public static final String X_KEY = "x";
  public static final String Y_KEY = "y";
  public static final String Z_KEY = "z";

  public void storeA() {
    SharedPreferences prefs = getSharedPreferences(DATA_KEY,
                                                   MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.putFloat(X_KEY,
                    lastX);
    editor.putFloat(Y_KEY,
                    lastY);
    editor.putFloat(Z_KEY,
                    lastZ);
    editor.commit();
  }


  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if(aCounter < 40) {
      aCounter++;
      return;
    }
    aCounter = 0;
    lastX = sensorEvent.values[0];
    lastY = sensorEvent.values[1];
    lastZ = sensorEvent.values[2];
    String row = "A," + System.currentTimeMillis() + "," +
      lastX + "," +
      lastY + "," +
      lastZ + "\n";
    Log.w("write",
          row);
    storeA();
    writeRow(row);
  }

  public void writeRow(final String row) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... nothing) {
        try {
          fos.write(row.getBytes());
        }
        catch(FileNotFoundException e) {
          e.printStackTrace();
        }
        catch(IOException e) {
          e.printStackTrace();
        }
        return null;
      }
    }.execute();
  }

  @Override
  public void onAccuracyChanged(Sensor sensor,
                                int i) {
    //Don't care
  }
}
