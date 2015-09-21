package topactors.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import topactors.shared.BestActor;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;

public class BestActors extends Composite {
  
  public void update() {
    int maxSize = _list.size() < _size ? _list.size() : _size;
    _table.setVisibleRange(0, maxSize);
    // System.err.println("Best actor list size : " + _list.size());
    Collections.sort(_list);
    _provider.refresh();
    _table.redraw();
  }

  public void remove(BestActor A) {
    _list.remove(_id_map.get(A._id));
  }
  
  public void add(BestActor A) {
    BestActor old = _id_map.put(A._id, A);
    _list.remove(old);
    _list.add(A);
  }
  
  private static NumberFormat _SAT_FORMAT = NumberFormat.getFormat("0.00");
  

  public @UiConstructor BestActors(int size) {
    _size = size;
    final Column<BestActor, String> nameColumn = new Column<BestActor, String>(new ClickableTextCell()) {
      @Override
      public String getValue(BestActor A) { return A._name == null ? "null" : A._name; }
    };
    nameColumn.setFieldUpdater(new FieldUpdater<BestActor, String>() {
      @Override
      public void update(int index, BestActor A, String value) {
        Details.instance().fetch(A._id, A._name);
      }
    });
    final TextColumn<BestActor> genderColumn = new TextColumn<BestActor>() {
      @Override
      public String getValue(BestActor A) { return A._gender.getCode(); }
    };
    final TextColumn<BestActor> scoreColumn = new TextColumn<BestActor>() {
      @Override
      public String getValue(BestActor A) { return _SAT_FORMAT.format(A._sat); }
    };
    final TextColumn<BestActor> movieColumn = new TextColumn<BestActor>() {
      @Override
      public String getValue(BestActor A) { return "" + A._movies; }
    };
    final TextColumn<BestActor> processColumn = new TextColumn<BestActor>() {
      @Override
      public String getValue(BestActor A) { return "" + (A._movies - A._movies_unprocessed); }
    };
    final TextColumn<BestActor> starColumn = new TextColumn<BestActor>() {
      @Override
      public String getValue(BestActor A) { return "" + A._stars; }
    };
    final TextColumn<BestActor> oscarColumn = new TextColumn<BestActor>() {
      @Override
      public String getValue(BestActor A) { return "" + A._oscars; }
    };

    _provider.addDataDisplay(_table);
    _table.addColumn(nameColumn, "Name");
    _table.addColumn(genderColumn, "Sex");
    _table.addColumn(scoreColumn, "SAT");
    _table.addColumn(movieColumn, "Movies");
    _table.addColumn(processColumn, "Proc");
    _table.addColumn(starColumn, "Star");
    _table.addColumn(oscarColumn, "Oscar");

    _table.setWidth("450px", true);
    _table.setColumnWidth(nameColumn, 150, Unit.PX);
    _table.setColumnWidth(genderColumn, 40, Unit.PX);
    _table.setColumnWidth(scoreColumn, 60, Unit.PX);
    _table.setColumnWidth(movieColumn, 50, Unit.PX);
    _table.setColumnWidth(processColumn, 50, Unit.PX);
    _table.setColumnWidth(starColumn, 50, Unit.PX);
    _table.setColumnWidth(oscarColumn, 50, Unit.PX);
    
    initWidget(_table);
  }
  
  public int getSize() { return _size; }
  
  private int _size = 0;
  private ProvidesKey<BestActor> _keyProvider = new ProvidesKey<BestActor>() {
    @Override public Object getKey(BestActor A) { return (A == null) ? null : A._id; }
  };
  private HashMap<String, BestActor> _id_map = new HashMap<String, BestActor>();
  private CellTable<BestActor> _table = new CellTable<BestActor>(_keyProvider);
  private ArrayList<BestActor> _list = new ArrayList<BestActor>();
  private ListDataProvider<BestActor> _provider = new ListDataProvider<BestActor>(_list);
}
