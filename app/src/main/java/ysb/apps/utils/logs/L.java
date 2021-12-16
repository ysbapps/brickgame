package ysb.apps.utils.logs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class L
{
  private static final SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH);
  private static final ArrayList<LR> logs = new ArrayList<>();


  public static void i(Object... objs)
  {
    add(false, objs);
  }

  public static void w(Object... objs)
  {
    add(true, objs);
  }

  private static void add(boolean warn, Object... objs)
  {
    LR lr = new LR();
    lr.warn = warn;
    lr.time = df.format(new Date());
    StringBuilder sb = new StringBuilder();
    if (objs != null)
      for (Object obj : objs)
        sb.append(obj).append(", ");
    else
      sb.append("null");

    int end = sb.lastIndexOf(", ");
    lr.text = end != -1 ? sb.substring(0, end) : sb.toString();

    logs.add(lr);
    System.out.println((lr.warn ? "WARN " : "INFO ") + lr.time + "  " + lr.text);
  }

  public static ArrayList<LR> logs()
  {
    return logs;
  }
}
