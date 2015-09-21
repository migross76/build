package topactors.client;

import java.util.ArrayList;
import topactors.shared.ActorDetail;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.view.client.ListDataProvider;

public class Details {
  private static DetailsUiBinder uiBinder = GWT.create(DetailsUiBinder.class);
  interface DetailsUiBinder extends UiBinder<DialogBox, Details> { /*binder*/ }
  
  private DialogBox _main = null;


  private static final Details _instance = new Details();
  public static Details instance() { return _instance; }

  public void fetch(String actorID, String actorName) {
    _main.setText("Details for " + actorName);
    actorService.getActorDetails(actorID, _getDetail);
  }
  
  private static NumberFormat _SAT_FORMAT = NumberFormat.getFormat("0.00");

  private Details() {
    _main = uiBinder.createAndBindUi(this);
    _main.setAnimationEnabled(true); // Enable animation.
    _main.setGlassEnabled(true); // Enable glass background.

    final TextColumn<ActorDetail> processedColumn = new TextColumn<ActorDetail>() {
      @Override
      public String getValue(ActorDetail A) { return A._processed ? "x" : " "; }
    };
    final TextColumn<ActorDetail> nameColumn = new TextColumn<ActorDetail>() {
      @Override
      public String getValue(ActorDetail A) { return A._name; }
    };
    final TextColumn<ActorDetail> seriesColumn = new TextColumn<ActorDetail>() {
      @Override
      public String getValue(ActorDetail A) { return "" + A._series; }
    };
    final TextColumn<ActorDetail> scoreColumn = new TextColumn<ActorDetail>() {
      @Override
      public String getValue(ActorDetail A) { return _SAT_FORMAT.format(A._score); }
    };
    final TextColumn<ActorDetail> satColumn = new TextColumn<ActorDetail>() {
      @Override
      public String getValue(ActorDetail A) { return _SAT_FORMAT.format(A._sat); }
    };
    final TextColumn<ActorDetail> starColumn = new TextColumn<ActorDetail>() {
      @Override
      public String getValue(ActorDetail A) { return A._star ? "*" : " "; }
    };
    final TextColumn<ActorDetail> specialColumn = new TextColumn<ActorDetail>() {
      @Override
      public String getValue(ActorDetail A) {
        StringBuilder SB = new StringBuilder();
        switch (A._oscar_role) {
          case WINNER: SB.append("W"); break;
          case NOMINATED: SB.append("N"); break;
          case NONE: break;
        }
        switch (A._oscar_movie) {
          case WINNER: SB.append("w"); break;
          case NOMINATED: SB.append("n"); break;
          case NONE: break;
        }
        if (A._voice) { SB.append("V"); }
        if (A._self) { SB.append("S"); }
        return SB.toString();
      }
    };
    _provider.addDataDisplay(_table);
    _table.addColumn(processedColumn, "P");
    _table.addColumn(nameColumn, "Name");
    _table.addColumn(seriesColumn, "@");
    _table.addColumn(starColumn, "*");
    _table.addColumn(specialColumn, "?");
    _table.addColumn(scoreColumn, "Score");
    _table.addColumn(satColumn, "SAT");

    _table.setWidth("auto", true);
    _table.setColumnWidth(processedColumn, 45, Unit.PX);
    _table.setColumnWidth(nameColumn, 220, Unit.PX);
    _table.setColumnWidth(seriesColumn, 45, Unit.PX);
    _table.setColumnWidth(starColumn, 40, Unit.PX);
    _table.setColumnWidth(specialColumn, 60, Unit.PX);
    _table.setColumnWidth(scoreColumn, 60, Unit.PX);
    _table.setColumnWidth(satColumn, 60, Unit.PX);

  }

  /** @param e mark this as a click handler */
  @UiHandler("_ok")
  void handleClick(ClickEvent e) { _main.hide(); }

  private static final ActorServiceAsync actorService = GWT.create(ActorService.class);

  private AsyncCallback<ArrayList<ActorDetail>> _getDetail = new AsyncCallback<ArrayList<ActorDetail>>() {
    @Override
    public void onFailure(Throwable caught) { System.err.println("Error : " + caught); }

    @Override
    public void onSuccess(ArrayList<ActorDetail> result) {
      _list.clear();
      _list.addAll(result);
      _table.setVisibleRange(0, result.size());
      _provider.refresh();
      _table.redraw();
      _main.setPopupPositionAndShow(new DialogBox.PositionCallback() {
        @Override public void setPosition(int offsetWidth, int offsetHeight) {
          int left = (Window.getClientWidth() - offsetWidth) / 2;
          int top = (Window.getClientHeight() - offsetHeight) / 2;
          _main.setPopupPosition(left, top);
        }
      });
      _main.center();
    }
  };
  
  @UiField CellTable<ActorDetail> _table;
  private ArrayList<ActorDetail> _list = new ArrayList<ActorDetail>();
  private ListDataProvider<ActorDetail> _provider = new ListDataProvider<ActorDetail>(_list);
}
