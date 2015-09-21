package topactors.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import topactors.shared.NextInfo;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.ListDataProvider;

public class NextTable extends Composite {
  public void update(boolean sort) {
    int maxSize = _list.size() < _size ? _list.size() : _size;
    _table.setVisibleRange(0, maxSize);
    // System.err.println("Next list size : " + _list.size());
    if (sort) { Collections.sort(_list); }
    _provider.refresh();
    _table.redraw();
  }

  public NextInfo next() { return _list.get(0); }
  
  public void remove(String id) {
    NextInfo old = _id_map.get(id);
    if (old != null) { _list.remove(old); }
  }
  
  public void update(NextInfo N) {
    NextInfo old = _id_map.put(N._id, N);
    if (old != null) { _list.remove(old); }
    _list.add(N);
  }
  
  private static NumberFormat _SAT_FORMAT = NumberFormat.getFormat("0.00");

  public @UiConstructor NextTable(int size) {
    _size = size;
    final TextColumn<NextInfo> nameColumn = new TextColumn<NextInfo>() {
      @Override
      public String getValue(NextInfo A) { return A._name; }
    };
    final TextColumn<NextInfo> scoreColumn = new TextColumn<NextInfo>() {
      @Override
      public String getValue(NextInfo A) { return _SAT_FORMAT.format(A._sat); }
    };
    final TextColumn<NextInfo> occurColumn = new TextColumn<NextInfo>() {
      @Override
      public String getValue(NextInfo A) { return "" + A._occurrences; }
    };
    _provider.addDataDisplay(_table);
    _table.addColumn(nameColumn, "Name");
    _table.addColumn(occurColumn, "#");
    _table.addColumn(scoreColumn, "SAT");
    
    _table.setWidth("550px", true);
    _table.setColumnWidth(nameColumn, 300, Unit.PX);
    _table.setColumnWidth(occurColumn, 50, Unit.PX);
    _table.setColumnWidth(scoreColumn, 70, Unit.PX);
    initWidget(_table);
  }
  
  public int getSize() { return _size; }
  
  private int _size = 0;
  private HashMap<String, NextInfo> _id_map = new HashMap<String, NextInfo>();
  private CellTable<NextInfo> _table = new CellTable<NextInfo>();
  private ArrayList<NextInfo> _list = new ArrayList<NextInfo>();
  private ListDataProvider<NextInfo> _provider = new ListDataProvider<NextInfo>(_list);
}
