package com.tokbox.android.profiler.metrics;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.tokbox.android.CPUStatListener;
import com.tokbox.android.profiler.utils.CPUStatUtils;
import com.tokbox.android.profiler.utils.LogReport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class CPUStat {

  CPUStatListener listener;

  private final static String LOG_TAG = "cpu-profiler";
  public static String OUTPUT_FILE = "cpu_usage_2.5.0.txt";

  private static final int CPU_INTERVAL = 100; //time between reading and computing CPU usage
  private static final int CPU_REFRESH_RATE = 100; //time between each CPU load measurement

  private Context mContext;

  private LogReport mOutputFile;

  private ArrayList<CpuInfo> mCpuInfoStats;
  private ArrayList<CpuInfo> mStatInterval;
  private static HandlerThread mHandlerThreadCPU;

  private static boolean monitorCpu;


  public final class CpuInfo {
    protected float totalCpu;
    protected float pidCpu;

    public CpuInfo(float totalCpu, float pidCpu) {
      this.totalCpu = totalCpu;
      this.pidCpu = pidCpu;
    }

    public float getTotalCpu() {
      return totalCpu;
    }

    public float getPidCpu() {
      return pidCpu;
    }
  }

  public CPUStat(Context context) {
    this.mContext = context;
    mCpuInfoStats = new ArrayList<CpuInfo>();
    mStatInterval = new ArrayList<CpuInfo>();
  }

  public void start() {
    monitorCpu = true;
    mHandlerThreadCPU = new HandlerThread("CPU monitoring");
    mHandlerThreadCPU.start();
    final int[] index = { 1 }; //interval index

    Handler handler = new Handler(mHandlerThreadCPU.getLooper());
    handler.post(new Runnable() {

      @Override
      public void run() {

        while (monitorCpu) {

          CPUStatUtils statUtils = new CPUStatUtils();
          //read /proc/stat file first time
          ArrayList<String> cpuStats1 = statUtils.parseCpuFile();

          //get pid of the process
          int pid = android.os.Process.myPid();

          if (cpuStats1.size() > 0) {
            //read proc/<pid>/stat
            String pidStat1 = statUtils.readProcessStat(pid);
             try {
              Thread.sleep(CPU_INTERVAL); //time slot of 100 ms to get proc/stat again
            } catch (Exception e) {
              e.printStackTrace();
            }
            //read /proc/stat file second time
            ArrayList<String> cpuStats2 = statUtils.parseCpuFile();

            if (cpuStats2 != null) {
              //read proc/<pid>/stat
              String pidStat2 = statUtils.readProcessStat(pid);
              //get total cpu usage as the avg of the cores cpu usage
              float total_cpu = statUtils.getSystemCpuUsage(cpuStats1, cpuStats2);

              //get process cpu usage
              long uptime = statUtils.getSystemUptimeDiff();
              float pid_cpu = statUtils.getProcessCpuUsage(pidStat1, pidStat2, uptime);

              if (pid_cpu >= 0.0f && total_cpu >= 0.0f) {
//                if ( index[0] == 10 ) { //get the cpu usage avg for 1 second
//                  CpuInfo statInfo = getAvgStat(mStatInterval);
//                  //write data in output file
//                  if (statInfo != null && mOutputFile != null) {
//                    mCpuInfoStats.add(statInfo);
//
//                    mOutputFile.getPrintStream()
//                        .println(Float.toString(statInfo.getTotalCpu()) + "," + Float.toString(statInfo.getPidCpu()));
//                  }
//                  index[0] = 1;
//                  mStatInterval = new ArrayList<CpuInfo>();
//                }
                CpuInfo statInfo = new CpuInfo(total_cpu,pid_cpu);
                if (statInfo != null && mOutputFile != null) {
                    mCpuInfoStats.add(statInfo);
                    mOutputFile.getPrintStream()
                        .println(Float.toString(statInfo.getTotalCpu()) + "," + Float.toString(statInfo.getPidCpu()));
                  }
                  //index[0] = 1;
                  mStatInterval = new ArrayList<CpuInfo>();
                mStatInterval.add(new CpuInfo(total_cpu, pid_cpu ));
                index[0]++;


//                Log.d(LOG_TAG, "#####################");
//                Log.d(LOG_TAG, "CPU TOTAL: "+total_cpu);
//                Log.d(LOG_TAG, "PID CPU: "+pid_cpu);
//                Log.d(LOG_TAG, "#####################");
                listener.onCPU(total_cpu,pid_cpu);

              }
            } //else cpu2
          }//else cpu1

          try {
            synchronized (this) {
              wait(CPU_REFRESH_RATE);
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
            return;
          }
        }
      }
    });
  }

  private CpuInfo getAvgStat(ArrayList<CpuInfo> stats) {
    float totalAvg = 0f;
    float processAvg = 0f;
    CpuInfo ret = null;

    for (int i=0; i < stats.size(); i++){
      totalAvg += stats.get(i).getTotalCpu();
      processAvg += stats.get(i).getPidCpu();
    }
    totalAvg = totalAvg / stats.size();
    processAvg = processAvg / stats.size();
    ret = new CpuInfo(totalAvg, processAvg);

    return ret;
  }

  public void stop() {
    if (mHandlerThreadCPU != null) {
      monitorCpu = false;
      mHandlerThreadCPU.quit();
      mHandlerThreadCPU = null;
    }
    mOutputFile.generateCSVFile(); //generate CSV file
    mOutputFile = null;
  }

  public void setLogOutput(File dir, boolean append) {
    try {
      File file = new File(dir, OUTPUT_FILE);
      mOutputFile = new LogReport(file);
      mOutputFile.setOutputStream(new FileOutputStream(mOutputFile.getFile(), append));

      mOutputFile.getPrintStream().println("Total CPU % , PID CPU %");
      mOutputFile.flush();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public ArrayList<CpuInfo> getCpuInfoStats() {
    return mCpuInfoStats;
  }

  public float getTotalCpuAvg(){
    float total = 0f;

     for ( int i=0; i<mCpuInfoStats.size(); i++ ){
       CpuInfo cpuInfo = mCpuInfoStats.get(i);
       total += cpuInfo.getTotalCpu();
     }
    mOutputFile.getPrintStream().println("Total CPU Average: "+ total/mCpuInfoStats.size());
    return total/mCpuInfoStats.size();
  }

  public float getProcessCpuAvg(){
    float total = 0f;

    for ( int i=0; i<mCpuInfoStats.size(); i++ ){
      CpuInfo cpuInfo = mCpuInfoStats.get(i);
      total += cpuInfo.getPidCpu();
    }
    mOutputFile.getPrintStream().println("Total Process CPU Average: "+ total/mCpuInfoStats.size());
    this.stop();
    return total/mCpuInfoStats.size();
  }

  public void setStatListener(CPUStatListener listener){
    this.listener = listener;
  }

}
