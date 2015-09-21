package util;

import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

// Wrapper around POI to create Excel spreadsheets relatively easily
public class Excel {
  
  public Excel(String name) {
    _sheet = _wb.createSheet(name);
    row();
  }
  
  public Excel row() {
    _row = _sheet.createRow(++_row_id);
    _cell_id = -1;
    return this;
  }
  
  private Cell cell() {
    if (_columns < ++_cell_id) { _columns = _cell_id; }
    return _row.createCell(_cell_id);
  }
  
  public Excel add(int val) {
    cell().setCellValue(val);
    return this;
  }
  
  public Excel add(String... vals) {
    for (String val : vals) { cell().setCellValue(val); }
    return this;
  }
  
  public Excel skip(int cells) {
    _cell_id += cells;
    return this;
  }

  public Excel add(double val, int dec) {
    CellStyle style = _wb.createCellStyle();
    DataFormat format = _wb.createDataFormat();
    StringBuilder SB = new StringBuilder("0.");
    for (int i = 0; i != dec; ++i) { SB.append("0"); }
    style.setDataFormat(format.getFormat(dec == 0 ? "0" : SB.toString()));
    Cell C = cell();
    C.setCellValue(val);
    C.setCellStyle(style);
    return this;
  }
  
  public void setWidth() {
    for (int i = 0; i != _columns + 1; ++i) {
      _sheet.autoSizeColumn(i);
    }
  }

  private int _row_id = -1;
  private Row _row = null;
  private Sheet _sheet = null;
  int _cell_id = -1;
  int _columns = 0;
  
  private static Workbook _wb = new HSSFWorkbook();
  
  public static void save(String filename) throws IOException {
    try (FileOutputStream out = new FileOutputStream(filename)) {
      _wb.write(out);
    }
    _wb = new HSSFWorkbook();
  }

}
