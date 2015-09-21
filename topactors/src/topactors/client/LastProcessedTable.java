package topactors.client;

import java.util.ArrayList;
import topactors.shared.LastProcessed;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.ListDataProvider;

public class LastProcessedTable extends Composite {
  public void update() {
    int maxSize = _list.size() < _size ? _list.size() : _size;
    _table.setVisibleRange(0, maxSize);
    _provider.refresh();
    _table.redraw();
  }
  
  public void insert(LastProcessed LP) {
    if (LP == null) { return; }
    _list.add(0, LP);
    if (_list.size() > _size) { _list.remove(_list.size() - 1); }
  }
  
  private static NumberFormat _SAT_FORMAT = NumberFormat.getFormat("0.00");

  public @UiConstructor LastProcessedTable(int size) {
    _size = size;
    final TextColumn<LastProcessed> actorColumn = new TextColumn<LastProcessed>() {
      @Override
      public String getValue(LastProcessed LP) { return LP._actorName; }
    };
    final TextColumn<LastProcessed> movieColumn = new TextColumn<LastProcessed>() {
      @Override
      public String getValue(LastProcessed LP) { return LP._movieName; }
    };
    final TextColumn<LastProcessed> actorScoreColumn = new TextColumn<LastProcessed>() {
      @Override
      public String getValue(LastProcessed LP) { return _SAT_FORMAT.format(LP._actorSAT); }
    };
    final TextColumn<LastProcessed> roleScoreColumn = new TextColumn<LastProcessed>() {
      @Override
      public String getValue(LastProcessed LP) { return _SAT_FORMAT.format(LP._roleSAT); }
    };
    _provider.addDataDisplay(_table);
    _table.addColumn(actorColumn, "Actor");
    _table.addColumn(actorScoreColumn, "SAT");
    _table.addColumn(movieColumn, "Movie");
    _table.addColumn(roleScoreColumn, "SAT");

    _table.setWidth("550px", true);
    _table.setColumnWidth(actorColumn, 150, Unit.PX);
    _table.setColumnWidth(actorScoreColumn, 60, Unit.PX);
    _table.setColumnWidth(movieColumn, 200, Unit.PX);
    _table.setColumnWidth(roleScoreColumn, 60, Unit.PX);
    initWidget(_table);
  }
  
  public int getSize() { return _size; }
  
  private int _size = 0;
  private CellTable<LastProcessed> _table = new CellTable<LastProcessed>();
  private ArrayList<LastProcessed> _list = new ArrayList<LastProcessed>();
  private ListDataProvider<LastProcessed> _provider = new ListDataProvider<LastProcessed>(_list);
}
