package com.tokbox.android.profiler.metrics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.tokbox.android.profiler.utils.LogReport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class BatteryStat {

  private final static String LOG_TAG = "battery-profiler";
  public static String OUTPUT_FILE="battery_usage_2.5.0.txt";
  private Context mContext;

  private ArrayList<BatteryInfo> mBatteryInfoStats;
  private LogReport mOutputFile;

  public BatteryStat(Context context) {

    this.mContext = context;
    mBatteryInfoStats = new ArrayList<BatteryInfo>();
  }

  public final class BatteryInfo {

    protected String status; //current status
    protected String health; //current health constant.
    protected boolean isHealth; //boolean indicating if health value is good
    protected boolean present; //boolean indicating whether a battery is present.
    protected int level; //integer containing the maximum battery level.
    protected int scale; //integer containing the maximum battery level.
    protected float charge;
    protected float temperature; //temperature level
    protected float voltage; //battery voltage level. Reported in millivolts
    protected String plugged; //whether the device is plugged in to a power source
    protected int icon_small;
    protected String technology; //technology of the current battery.


    protected BatteryInfo(String status, String health, boolean isHealth, boolean present, int level, int scale, float charge, float temperature, float voltage, String plugged, int icon_small, String technology){
      this.status = status;
      this.health = health;
      this.isHealth = isHealth;
      this.present = present;
      this.level = level;
      this.scale = scale;
      this.charge = charge;
      this.temperature = temperature;
      this.voltage = voltage;
      this.plugged = plugged;
      this.icon_small = icon_small;
      this.technology = technology;
    }

    public String getStatus() {
      return status;
    }

    public String getHealth() {
      return health;
    }

    public boolean isHealth() {
      return isHealth;
    }

    public boolean isPresent() {
      return present;
    }

    public int getLevel() {
      return level;
    }

    public int getScale() {
      return scale;
    }

    public float getCharge() { return charge; }

    public float getTemperature() {
      return temperature;
    }

    public float getVoltage() {
      return voltage;
    }

    public String getPlugged() {
      return plugged;
    }

    public int getIcon_small() {
      return icon_small;
    }

    public String getTechnology() {
      return technology;
    }
  }

  public void start(){
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_BATTERY_CHANGED);

    Log.d(LOG_TAG, "Register battery status receiver.");
    mContext.registerReceiver(mBatteryTracker, filter);

  }

  public void stop() {
    Log.d(LOG_TAG, "Unegister battery status receiver.");

    mContext.unregisterReceiver(mBatteryTracker);

    mOutputFile.generateCSVFile(); //generate CSV file
  }

  public void setLogOutput(File dir, boolean append){
    try {
   File file = new File(dir, OUTPUT_FILE);
      mOutputFile = new LogReport(file);
      mOutputFile.setOutputStream(new FileOutputStream(mOutputFile.getFile(), append));

      //setting the header of the table
      mOutputFile.getPrintStream().println("Status , Health, Present, Level %(out of 100), Scale, Charge, Temperature, Voltage(mV), Plugged, Technology");
      mOutputFile.flush();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public ArrayList<BatteryInfo> getBatteryInfoStats() {
    return mBatteryInfoStats;
  }

  /**
   * BroadcastReceiver is used for receiving intents from the BatteryManager when the battery changed
   */
  private BroadcastReceiver mBatteryTracker = new BroadcastReceiver() {

    private boolean isHealth = false;
    public void onReceive(Context context, Intent intent) {

    String action = intent.getAction();

      // information received from BatteryManager
      if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
        Log.d(LOG_TAG, "Received battery status information");

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
        String statusStr = getStatusValue(status);
        Log.d(LOG_TAG, "Received battery status "+statusStr);

        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
        String healthStr = getHealthValue(health);
        boolean isHealth = isHealth(healthStr);
        Log.d(LOG_TAG, "Received battery healthStr "+healthStr);

        boolean present = intent.getBooleanExtra(
            BatteryManager.EXTRA_PRESENT, false);
        Log.d(LOG_TAG, "Received battery present "+present);

        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,
            0);
        String pluggedStr = getPluggedValue(plugged);
        Log.d(LOG_TAG, "Received battery pluggedStr "+pluggedStr);

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        Log.d(LOG_TAG, "Received battery level "+ Integer.toString(level));

        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
        Log.d(LOG_TAG, "Received battery scale "+ Integer.toString(scale));

        float batteryPct = level / (float)scale;
        Log.d(LOG_TAG, "Received battery percentage level/scale " + Float.toString(batteryPct));

        int icon_small = intent.getIntExtra(
            BatteryManager.EXTRA_ICON_SMALL, 0);
        int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,
            0);
        float mVoltage = (float)voltage/1000;
        Log.d(LOG_TAG, "Received battery mVoltage " + Float.toString(mVoltage));

        int temperature = intent.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE, 0);
        float mTemperature = (float)temperature/10; //temperature is reported in tenths of a degree Centigrade (from BatteryService.java)
        Log.d(LOG_TAG, "Received battery mTemperature " + Float.toString(mTemperature));

        String technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        if ( technology.isEmpty() ) {
          technology = "unknown";
        }
        Log.d(LOG_TAG, "Received battery technology " + technology);

        BatteryInfo batteryInfo = new BatteryInfo(statusStr, healthStr, isHealth, present, level, scale, batteryPct, mTemperature, mVoltage, pluggedStr, icon_small, technology );
        mBatteryInfoStats.add(batteryInfo);

        //store battery information
        mOutputFile.getPrintStream().println(statusStr + "," +healthStr + "," + Boolean.toString(present)+","+ Integer.toString(level) +","+Integer.toString(scale) +","+ Float.toString(batteryPct)+","+Float.toString(mTemperature) +","+ Float.toString(mVoltage)+","+ pluggedStr +","+ technology);

      }
    }
  };

  private String getStatusValue(int status) {

    String statusStr = null;

    switch (status) {
      case BatteryManager.BATTERY_STATUS_UNKNOWN:
        statusStr = "unknown";
        break;
      case BatteryManager.BATTERY_STATUS_CHARGING:
        statusStr = "charging";
        break;
      case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
        statusStr = "not charging";
        break;
      case BatteryManager.BATTERY_STATUS_DISCHARGING:
        statusStr = "discharging";
        break;
      case BatteryManager.BATTERY_STATUS_FULL:
        statusStr = "full";
        break;
    }

    return statusStr;
  }

  private String getHealthValue(int health) {
    String healthStr = null;

    switch (health) {
      case BatteryManager.BATTERY_HEALTH_UNKNOWN:
        healthStr = "unknown";
        break;
      case BatteryManager.BATTERY_HEALTH_GOOD:
        healthStr = "good";
        break;
      case BatteryManager.BATTERY_HEALTH_OVERHEAT:
        healthStr = "overheat";
        break;
      case BatteryManager.BATTERY_HEALTH_DEAD:
        healthStr = "dead";
        break;
      case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
        healthStr = "over voltage";
        break;
      case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
        healthStr = "unspecified failure";
        break;
    }
    return healthStr;
  }

  private boolean isHealth(String health){
    if (health.equals("good")){
      return true;
    }
    else {
      return false;
    }
  }

  private String getPluggedValue(int plugged){
    String acStr = null;

    switch (plugged) {
      case BatteryManager.BATTERY_PLUGGED_AC:
        acStr = "plugged AC";
        break;
      case BatteryManager.BATTERY_PLUGGED_USB:
        acStr = "plugged USB";
        break;
      default:
        acStr = "not plugged";
    }
    return acStr;
  }

}
