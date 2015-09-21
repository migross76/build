package topactors.shared;

public enum Gender {
  Female(2.0, "F"), Male(1.0, "M"), Unknown(1.0, "?");
  Gender(double weight, String code) { _weight = weight; _code = code; }
  public double getWeight() { return _weight; }
  public String getCode() { return _code; }
  private double _weight = 0;
  private String _code = null;
}