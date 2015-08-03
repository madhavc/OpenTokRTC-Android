package com.tokbox.android.profiler;

import android.content.Context;

import com.tokbox.android.CPUStatListener;
import com.tokbox.android.MemStatListener;
import com.tokbox.android.opentokrtc.ChatRoomActivity;
import com.tokbox.android.profiler.metrics.BatteryStat;
import com.tokbox.android.profiler.metrics.CPUStat;
import com.tokbox.android.profiler.metrics.MemStat;

import java.io.File;


public class PerformanceProfiler {
  private final static String LOG_TAG = "performance-profiler";
  private final static File OUTPUT_DIR = android.os.Environment.getExternalStorageDirectory();

  private Context context;
  protected BatteryStat mBatteryStats;
  protected CPUStat mCpuStats;
  protected MemStat mMemStats;
  private ChatRoomActivity chatRoomActivity;

  public PerformanceProfiler(Context ctx , ChatRoomActivity currentActivity){
    this.context = ctx;
    this.chatRoomActivity = currentActivity;

  }

  public void release() {
    mBatteryStats = null;
    mCpuStats = null;
    mMemStats = null;
  }

  public void startBatteryStat(String fileName) {
    mBatteryStats = new BatteryStat(this.context);

    mBatteryStats.OUTPUT_FILE = fileName;
    mBatteryStats.setLogOutput(OUTPUT_DIR, false);
    mBatteryStats.start();
  }

  public void stopBatteryStat() {
    mBatteryStats.stop();
  }

  public void startCPUStat() {
    mCpuStats = new CPUStat(this.context);
    mCpuStats.setStatListener(new CPUStatListener() {
      @Override
      public void onCPU(float CPU, float Process) {
        //Log.d(LOG_TAG,"onCPU Got Called");
        chatRoomActivity.updateCPUStat(CPU, Process);
      }

    });
    mCpuStats.start();
  }

  public void stopCPUStat() {
    mCpuStats.stop();
  }

  public void startMemStat() {
    mMemStats = new MemStat(this.context);
    mMemStats.setMemoryStatListener(new MemStatListener() {
      @Override
      public void onMemoryStat(double available_mem, double total_mem, double used_mem, double used_per) {
        chatRoomActivity.updateMemStat(available_mem,total_mem,used_mem,used_per);
      }
    });
    mMemStats.start();
  }

  public void stopMemStat() {
    mMemStats.stop();
  }

  public BatteryStat getBatteryStat() {
    return mBatteryStats;
  }

  public CPUStat getCPUStat() {
    return mCpuStats;
  }

  public MemStat getMemStat() { return mMemStats; }


}
