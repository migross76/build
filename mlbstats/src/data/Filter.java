package data;

public interface Filter<T> {
  public boolean satisfied(T t);
}
