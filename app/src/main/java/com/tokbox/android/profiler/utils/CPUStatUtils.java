package com.tokbox.android.profiler.utils;

import android.util.Log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.regex.Pattern;

public final class CPUStatUtils {
  private final static String LOG_TAG = "cpu-stat-utils";

  public int numCores;
  public ArrayList<Long> systemUptimeStartCores;
  public ArrayList<Long> systemUptimeEndCores;

  public CPUStatUtils() {
    systemUptimeStartCores = new ArrayList<Long>();
    systemUptimeEndCores = new ArrayList<Long>();
  }

  /**
   * Return the first line of /proc/stat
   */
  public String readSystemStat() {
    RandomAccessFile reader = null;
    String load = null;
    try {
      reader = new RandomAccessFile("/proc/stat", "r");
      load = reader.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return load;
  }

  /**
   * Return the first line of /proc/pid/stat
   */
  public String readProcessStat(int pid) {

    RandomAccessFile reader = null;
    String line = null;
    try {
      reader = new RandomAccessFile("/proc/" + pid + "/stat", "r");
      line = reader.readLine();
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return line;
  }

  /**
   * Parse the proc/stat file
   */
  public ArrayList<String> parseCpuFile() {
    numCores = 0;
    RandomAccessFile reader = null;
    String load = null;
    ArrayList<String> cpuStats = new ArrayList<String>();

    try {
      reader = new RandomAccessFile("/proc/stat", "r");

      reader.seek(0);
      String line = "";
      do {
        line = reader.readLine();
        String cpuLine = parseCpuLine(line);
        if (cpuLine != null){
          cpuStats.add(cpuLine);
        }
      } while (line != null);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return cpuStats;
  }

  private String parseCpuLine(String cpuLine) {
    if (cpuLine != null && cpuLine.length() > 0) {
      String[] parts = cpuLine.split("[ ]+");
      String cpuLabel = "cpu";
      if ( parts[0].contains(cpuLabel)) {
        if (parts[0].matches("cpu.*[0-9]")){
          numCores++;
        }
        return cpuLine;
      }
    }
    return null;
  }

  /**
   * Return number of cores of the device
   */
  public int getNumCores() {
    return numCores;
  }

  /**
   * Calculate the total cpu usage in a specific time slot between the first and second read data
   * @param start first read data from proc/stat
   * @param end   second read data from proc/stat
   * @return percentage of the total cpu usage
   */
  public float getSystemCpuUsage(ArrayList<String> start, ArrayList<String> end) {
    float totalCpu = -1f;
    ArrayList<Float> cpuCores = new ArrayList<Float>();

    for( int i=1; i<=numCores; i++) {
      String startCpu = start.get(i);
      String endCpu = end.get(i);

      float cpuCore = getSystemCpuUsagePerCore(startCpu, endCpu);
      cpuCores.add(cpuCore);
    }

    totalCpu = getAvgCpuCores(cpuCores);
    return totalCpu;
  }

  /**
   * Return the percentage of the total CPU usage per core
   * @param start first read data of core X from proc/stat
   * @param end   second read data of core X from proc/stat
   * @return total cpu usage per core
   */
  private float getSystemCpuUsagePerCore(String start, String end) {
    String[] previous = start.split(" ");
    ArrayList<Long> startData = getValuesFromStat(previous);
    String[] current = end.split(" ");
    ArrayList<Long> endData = getValuesFromStat(current);

    //check overflow
    endData = diffWithOverflow(startData, endData);

    long idle1 = getSystemIdleTimePerCore(startData);
    long up1 = getSystemUptimePerCore(startData);
    systemUptimeStartCores.add(up1);
    long total1 = up1 + idle1;

    long idle2 = getSystemIdleTimePerCore(endData);
    long up2 = getSystemUptimePerCore(endData);
    systemUptimeEndCores.add(up2);

    long total2 = up2 + idle2;
    float cpu = -1f;

    if (idle1 >= 0 && up1 >= 0 && idle2 >= 0 && up2 >= 0) {
      if (total2-total1 > 0) {
        cpu = (100.0f * (up2 - up1)) / (float) (total2 - total1);
      }
    }
    return cpu;
  }

  /**
   * Calculate the average of the cpu cores usage
   * @param cpuCores
   * @return average of the cpu cores in percent
   */
  private float getAvgCpuCores(ArrayList<Float> cpuCores){
    float ret=-1f;
    float total= -1f;
    for( int i=0; i < cpuCores.size(); i++) {
      total += cpuCores.get(i);
    }

    ret = total/numCores;

    return ret;
  }

  /**
   * Return the sum of uptimes read from /proc/stat.
   */
  private long getSystemUptimePerCore(ArrayList<Long> stat) {

    long l = 0L;
    for (int i = 0; i < stat.size(); i++) {
      if (i != 2) { // bypass any idle mode. There is currently only one.
        try {
          l += stat.get(i);
        } catch (NumberFormatException ex) {
          Log.e(LOG_TAG, "Failed to parse stats", ex);
          return -1L;
        }
      }
    }

    return l;
  }

  /**
   * Return the difference between the system uptime for start data and end data
   */
  public long getSystemUptimeDiff() {

    long totalStart = 0L;
    long totalEnd = 0L;

    for (int i = 0; i < numCores; i++) {
      totalStart += systemUptimeStartCores.get(i);
      totalEnd += systemUptimeEndCores.get(i);
    }

    return ((totalEnd - totalStart) / numCores);
  }


  /**
   * Return the sum of system uptime from /proc/stat.
   */
  private long getProcessUptime(ArrayList<Long> stat) {
    return (stat.get(12) + stat.get(13));
  }

  /**
   * Return the sum of idle times read from /proc/stat.
   */
  private long getSystemIdleTimePerCore(ArrayList<Long> stat) {
    try {
      return stat.get(2);
    } catch (NumberFormatException ex) {
      ex.printStackTrace();
    }

    return -1L;
  }

  /**
   * Check the overflow case
   */
  private ArrayList<Long> diffWithOverflow(ArrayList<Long> previous, ArrayList<Long> current) {
    //TODO: Support 64bits device

    ArrayList<Long> ret = new ArrayList<Long>();
    for (int i=0; i< previous.size(); i++) {
      if (current.get(i) < previous.get(i)) {
        long value = current.get(i) + 0x100000000L;
        ret.add(value);
      }
      else {
        ret.add(current.get(i));
      }
    }
    return ret;
  }

  private boolean checkIsNumeric(String str) {
    Pattern numberPattern = Pattern.compile("-?\\d+");
    return (str != null && numberPattern.matcher(str).matches());
  }

  /**
   * Parse the cpu line from proc/stat to remove the string
   * @param stat
   * @return long values of the proc/stat line
   */
  private ArrayList<Long> getValuesFromStat(String [] stat) {
    ArrayList<Long> data = new ArrayList<Long>();
    int index = 0;

    for (int i = 0; i < stat.length; i++) {

      if (checkIsNumeric(stat[i])){
        data.add(Long.valueOf(stat[i]));
        index++;
      }
    }

    return data;
  }

  /**
   * Return the CPU usage of the process, in percent.
   * @param start first read data from /proc/pid/stat
   * @param end second read data from /proc/pid/stat
   * @param uptime difference between the system uptime of the start and end data
   * @return
   */
  public float getProcessCpuUsage(String start, String end, long uptime) {
    String[] stat = start.split(" ");

    ArrayList<Long> startData = getValuesFromStat(stat);
    String[] current = end.split(" ");

    ArrayList<Long> endData = getValuesFromStat(current);

    //check overflow
    endData = diffWithOverflow(startData, endData);
    long up1 = getProcessUptime(startData);
    long up2 = getProcessUptime(endData);

    float ret = -1f;
    if ((up1 >= 0) && (up2 >= up1) && (uptime > 0.)) {
      ret = (100.f * (up2 - up1)) / (float) uptime;
    }

    return ret;
  }


}