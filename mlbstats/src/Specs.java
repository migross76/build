import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Specs extends DefaultHandler {

  public void load(String config_file) throws Exception {
    SAXParserFactory.newInstance().newSAXParser().parse(new File(config_file), this);
  }

  public ArrayList<Spot> _spots = new ArrayList<>();
  public double _teamweight = 1.0;

  @Override public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if ("position".equals(qName)) {
      Spot S = new Spot();
      S._pos = atts.getValue("name");
      String temp = atts.getValue("playing-time");
      if (temp != null) { S._playingtime = Double.parseDouble(temp); }
      _spots.add(S);
    } else if ("team-weight".equals(qName)) {
      _teamweight = Double.parseDouble(atts.getValue("value"));
    }
  }
}
