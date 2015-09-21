package data;

public interface Groupable<T> {
  public void add(T t);
  
  public T create();
}
