package strat.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;

public abstract class BaseCallback implements RequestCallback {
  protected abstract void parseJSON(JSONValue js);
  
  @Override public final void onError(Request request, Throwable exception) {
    Window.alert(exception.toString());
    exception.printStackTrace(System.err);
  }

  @Override public final void onResponseReceived(Request request, Response response) {
    if (200 == response.getStatusCode()) {
      parseJSON(JSONParser.parseStrict(response.getText()));
    } else {
      Window.alert(response.getStatusCode() + " : " + response.getStatusText());
      System.err.println(response.getStatusCode() + " : " + response.getStatusText());
    }
  }
}
