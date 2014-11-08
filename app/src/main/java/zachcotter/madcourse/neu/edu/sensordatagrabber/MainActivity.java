package zachcotter.madcourse.neu.edu.sensordatagrabber;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity implements OnClickListener{

  private final static String SERVICE_NAME = "zachcotter.madcourse.neu.edu.sensordatagrabber.service.DATA_SERVICE";

  private boolean visible;
  private TextView distanceView;
  private TextView accelerationView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    findViewById(R.id.start_button).setOnClickListener(this);
    findViewById(R.id.stop_button).setOnClickListener(this);
    findViewById(R.id.submit_button).setOnClickListener(this);
    findViewById(R.id.start_over_button).setOnClickListener(this);
    distanceView = (TextView) findViewById(R.id.distance_view);
    accelerationView = (TextView) findViewById(R.id.gravity_view);
  }

  @Override
  public void onClick(View view) {
    switch(view.getId()){
      case R.id.start_button:
        start();
        return;
      case R.id.stop_button:
        stop();
        return;
      case R.id.submit_button:
        submit();
        return;
      case R.id.start_over_button:
        startOver();
        return;
    }
  }

  private void start(){
    //start the sensor
    startService(new Intent(SERVICE_NAME));
    findViewById(R.id.start_button).setVisibility(View.GONE);
  }

  private void stop(){
    //stop the sensor
    stopService(new Intent(SERVICE_NAME));
    findViewById(R.id.stop_button).setVisibility(View.GONE);
  }

  private void submit(){
    String expDistance = ((EditText) findViewById(R.id.expected_distance_field)).getText().toString();
    SharedPreferences prefs = getSharedPreferences(DataCollectionService.DATA_KEY, MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.remove(DataCollectionService.DISTANCE_KEY);
    editor.commit();
    File internalFile = getFileStreamPath(DataCollectionService.FILE_NAME);
    Uri uri = Uri.fromFile(internalFile);
    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_EMAIL,
               new String[]{"zach@zachcotter.com"});
    i.putExtra(Intent.EXTRA_SUBJECT,"Sensor Data");
    i.putExtra(Intent.EXTRA_TEXT, "Here is my sensor data.\n" +
      "I walked from:\n" +
      "I walked to:\n" +
      "Try and describe when and where you stopped:\n" +
      "Expected Distance: " + expDistance);
    i.putExtra(Intent.EXTRA_STREAM, uri);
    Log.w("uri", uri.toString());
    startActivity(Intent.createChooser(i,
                                       "Select application"));
  }

  private void startOver(){
    SharedPreferences prefs = getSharedPreferences(DataCollectionService.DATA_KEY, MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.remove(DataCollectionService.DISTANCE_KEY);
    editor.commit();
    finish();
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    visible = hasFocus;
    if(visible) {
      startWatchingSensor();
    }
  }

  private String gravityDisplay(float g){
    return Math.round(g / 9.81) + " gravities\n";
  }

  private void startWatchingSensor() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        while(visible) {
          try {
            Thread.sleep(2000);
          }
          catch(InterruptedException e) {
            e.printStackTrace();
          }
          finally {
            SharedPreferences prefs = getSharedPreferences(DataCollectionService.DATA_KEY,
                                                           MODE_PRIVATE);
            final float distance = prefs.getFloat(DataCollectionService.DISTANCE_KEY,
                                                  0);
            final float x = prefs.getFloat(DataCollectionService.X_KEY, 0);
            final float y = prefs.getFloat(DataCollectionService.Y_KEY, 0);
            final float z = prefs.getFloat(DataCollectionService.Z_KEY, 0);
            distanceView.post(new Runnable() {
              @Override
              public void run() {
                distanceView.setText("Distance: " + distance + " meters");
              }
            });
            accelerationView.post(new Runnable() {
              @Override
              public void run() {
                accelerationView.setText("Acceleration:\n" +
                                        "x:" + gravityDisplay(x) + "y:" + gravityDisplay(y) + "z:" + gravityDisplay(z));
              }
            });
          }

        }
      }
    }).start();
  }
}
