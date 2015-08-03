package com.tokbox.android.profiler.metrics;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.tokbox.android.MemStatListener;
import com.tokbox.android.profiler.utils.LogReport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class MemStat {

  MemStatListener listener;

  private static final int MEMORY_REFRESH_RATE = 100; // time between each memory load measurement

  private final static String LOG_TAG = "mem-profiler";
  public static String OUTPUT_FILE="mem_usage_2.5.0.txt";

  private Context mContext;

  private LogReport mOutputFile;


  private ArrayList<MemInfo> mMemInfoStats;

  private static HandlerThread mHandlerThreadMEM;

  private static boolean monitorMemory;

  public final class MemInfo {
    protected double totalMem;
    protected double freeMem;
    protected double usedMem;
    protected double usedMem_per;

    public MemInfo(double totalMem, double freeMem, double usedMem, double usedMem_per) {
      this.totalMem = totalMem;
      this.freeMem = freeMem;
      this.usedMem = usedMem;
      this.usedMem_per = usedMem_per;
    }

    public double getTotalMem() {
      return totalMem;
    }

    public double getFreeMem() {
      return freeMem;
    }

    public double getUsedMem() {
      return usedMem;
    }

    public double getUsedMem_per() {
      return usedMem_per;
    }
  }
  public MemStat(Context context) {
    this.mContext = context;
    mMemInfoStats = new ArrayList<MemInfo>();
  }

  public void start(){

    Log.d(LOG_TAG, "Starting memory monitoring ");
    monitorMemory = true;
    mHandlerThreadMEM = new HandlerThread("Memory monitoring");
    mHandlerThreadMEM.start();

    Handler handler = new Handler(mHandlerThreadMEM.getLooper());
    handler.post(new Runnable() {

      @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
      @Override
      public void run() {
        while (monitorMemory) {

          double mb=1048576.0;

          try {
            //Java memory usage
            Runtime info = Runtime.getRuntime();
            double freeSize_mb = (info.freeMemory() / mb);
            double totalSize_mb =  (info.totalMemory() / mb);
            double usedSize = (totalSize_mb  - freeSize_mb);

            //RAM memory usage
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            double available_mem = mi.availMem / mb;
            double total_mem = mi.totalMem / mb;
            double used_mem = (mi.totalMem - mi.availMem )/ mb;
            double used_per = (used_mem/ total_mem) *100;

            listener.onMemoryStat(available_mem,total_mem,used_mem,used_per);

//            Log.d(LOG_TAG, "available_mem: "+Double.toString(available_mem));
//            Log.d(LOG_TAG, "total_mem: "+Double.toString(total_mem));
//            Log.d(LOG_TAG, "used_mem: "+Double.toString(used_mem));
//            Log.d(LOG_TAG, "used_mem %: "+Double.toString(used_per));

            if ( mOutputFile != null ) {
              MemInfo memInfo = new MemInfo(total_mem, available_mem, used_mem, used_per);
              mMemInfoStats.add(memInfo);

              mOutputFile.getPrintStream()
                  .println("Available: "
                      + Double.toString(available_mem)
                      + ","
                      + "Total: "
                      + Double.toString(total_mem)
                      + ","
                      + "Used: "
                      + Double.toString(used_mem)
                      + ","
                      + "Used %: "
                      + Double.toString(used_per));
            }
            try {
              synchronized (this) {
                wait(MEMORY_REFRESH_RATE);
              }
            } catch (InterruptedException e) {
              e.printStackTrace();
              return;
            }

          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }

    });
  }

  public void stop() {
    Log.d(LOG_TAG, "Stopping memory monitoring ");

    if (mHandlerThreadMEM != null) {
      monitorMemory = false;
      mHandlerThreadMEM.quit();
      mHandlerThreadMEM = null;

    }
    mOutputFile.generateCSVFile();
    mOutputFile = null;
  }

  public void setLogOutput(File dir, boolean append){
    try {
      File file = new File(dir, OUTPUT_FILE);
      mOutputFile = new LogReport(file);
      mOutputFile.setOutputStream(new FileOutputStream(mOutputFile.getFile(), append));

      mOutputFile.getPrintStream().println("Free MEM , Total MEM , USED MEM, USED MEM %");
      mOutputFile.flush();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public ArrayList<MemInfo> getMemInfoStats() {
    return mMemInfoStats;
  }

  public double getUsedMemAvg(){
    double total = 0;

    for ( int i=0; i<mMemInfoStats.size(); i++ ){
      MemInfo memInfo = mMemInfoStats.get(i);
      total += memInfo.getUsedMem_per();
    }
    mOutputFile.getPrintStream().println("Total Used MEM Average: " + (double)(total/mMemInfoStats.size()));
    Log.d(LOG_TAG, "TOTAL USED MEM AVERAGE: " + (double)(total/mMemInfoStats.size()));
    this.stop();
    return (double)(total/mMemInfoStats.size());
  }

  public void setMemoryStatListener(MemStatListener listener){
    this.listener = listener;
  }
}
