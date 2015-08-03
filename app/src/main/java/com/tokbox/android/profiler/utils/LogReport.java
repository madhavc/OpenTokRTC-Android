package com.tokbox.android.profiler.utils;

/**
 * Created by mserrano on 16/02/15.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.StreamHandler;


public class LogReport extends StreamHandler {

  private OutputStream mOutputStream;
  private PrintStream mPrintStream;
  private File file;


  public LogReport(File file) {
    super();
    this.file = file;
  }

  @Override
  public void setOutputStream(OutputStream os) {
    super.setOutputStream(os);
    mOutputStream = os;
    mPrintStream = new PrintStream(os);
  }

  public OutputStream getOutputStream() {
    return mOutputStream;
  }

  public PrintStream getPrintStream() {
    return mPrintStream;
  }

  public File getFile() {
    return file;
  }

  public void generateCSVFile() {
    file.getName().split(".txt");
    File sdCardFile = new File(android.os.Environment.getExternalStorageDirectory()+"/"+file.getName().split(".txt")[0]+".csv");

    BufferedReader br;
    try {
      FileWriter writer = new FileWriter(sdCardFile, false);
      br = new BufferedReader(new FileReader(file));

      String line;
      line = br.readLine();

      //headers
      String [] data = line.split(",");
      for (int i=0; i<data.length; i++) {
        writer.append(data[i]);
        writer.append(',');
      }
      writer.append('\n');

      line = br.readLine();
      while (line != null) {
        data = line.split(",");

        for (int i=0; i<data.length; i++){
          writer.append(data[i]);
          writer.append(',');
        }
        writer.append('\n');
        line = br.readLine();
      }
      br.close();
      writer.flush();
      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}