package util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

public class Config2 {
  @SuppressWarnings("rawtypes")
  protected void init() throws IOException {
    Class C = this.getClass();
    Properties props = new Properties();
    props.load(C.getResourceAsStream(C.getSimpleName() + ".properties"));
    for (Map.Entry<Object, Object> E : props.entrySet()) {
      String key = E.getKey().toString();
      try {
        Field F = C.getField(key);
        if (F != null) {
          if (F.getType().equals(int.class)) {
            F.setInt(this, Integer.parseInt(E.getValue().toString()));
          } else if (F.getType().equals(double.class)) {
            F.setDouble(this, Double.parseDouble(E.getValue().toString()));
          } else if (F.getType().equals(boolean.class)) {
            F.setBoolean(this, Boolean.parseBoolean(E.getValue().toString()));
          } else {
            F.set(this, E.getValue());
          }
        }
      } catch (Throwable e) { /* skip it */ }
    }
  }

  public static class MyConfig extends Config2 {
    protected MyConfig() throws IOException {
      super();
      init();
    }

    public static int INT_VALUE = 0;
    public static double DBL_VALUE = 0;
    public static  String STR_VALUE = null;
    public static boolean BOOL_VALUE = true;
  }
  
  public static void main(String[] args) throws Exception {
//    MyConfig cfg = new MyConfig();
    System.out.println(MyConfig.INT_VALUE);
    System.out.println(MyConfig.DBL_VALUE);
    System.out.println(MyConfig.STR_VALUE);
    System.out.println(MyConfig.BOOL_VALUE ? "T" : "F");
  }
}
