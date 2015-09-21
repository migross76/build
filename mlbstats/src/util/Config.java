package util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Config {
  public Config(Class<? extends Object> cls) throws IOException {
    _props.load(cls.getResourceAsStream(cls.getSimpleName() + ".properties"));
  }

  public int getInt(String name) {
    return Integer.parseInt(_props.getProperty(name));
  }
  
  public String[] getStrings(String name) {
    return _props.getProperty(name).split(" ");
  }

  public List<String> getStringList(String name) {
    return Arrays.asList(getStrings(name));
  }

  private Properties _props = new Properties();
}
