package topactors.server;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Normalize {
  private Normalize ( ) { }
  
  private static HashMap<String,String> htmlEntities;
  static {
    htmlEntities = new HashMap<String,String>();
    htmlEntities.put("lt","<")    ; htmlEntities.put("gt",">");
    htmlEntities.put("amp","&")   ; htmlEntities.put("quot","\"");

    htmlEntities.put("aacute","�"); htmlEntities.put("Aacute","�");
    htmlEntities.put("agrave","�"); htmlEntities.put("Agrave","�");
    htmlEntities.put("acirc", "�"); htmlEntities.put("Acirc", "�");
    htmlEntities.put("auml",  "�"); htmlEntities.put("Auml",  "�");
    htmlEntities.put("aring", "�"); htmlEntities.put("Aring", "�");
    htmlEntities.put("aelig", "�"); htmlEntities.put("AElig", "�");
    htmlEntities.put("ccedil","�"); htmlEntities.put("Ccedil","�");
    htmlEntities.put("eacute","�"); htmlEntities.put("Eacute","�");
    htmlEntities.put("egrave","�"); htmlEntities.put("Egrave","�");
    htmlEntities.put("ecirc", "�"); htmlEntities.put("Ecirc", "�");
    htmlEntities.put("euml",  "�"); htmlEntities.put("Euml",  "�");
    htmlEntities.put("iacute","�"); htmlEntities.put("Iacute","�");
    htmlEntities.put("icirc", "�"); htmlEntities.put("Icirc", "�");
    htmlEntities.put("iuml",  "�"); htmlEntities.put("Iuml",  "�");
    htmlEntities.put("ntilde","�"); htmlEntities.put("Ntilde","�");
    htmlEntities.put("oacute","�"); htmlEntities.put("Oacute","�");
    htmlEntities.put("ocirc", "�"); htmlEntities.put("Ocirc", "�");
    htmlEntities.put("ouml",  "�"); htmlEntities.put("Ouml",  "�");
    htmlEntities.put("oslash","�"); htmlEntities.put("Oslash","�");
    htmlEntities.put("szlig", "�");
    htmlEntities.put("uacute","�"); htmlEntities.put("Uacute","�");
    htmlEntities.put("ugrave","�"); htmlEntities.put("Ugrave","�");
    htmlEntities.put("ucirc", "�"); htmlEntities.put("Ucirc", "�");
    htmlEntities.put("uuml",  "�"); htmlEntities.put("Uuml",  "�");
    
    htmlEntities.put("nbsp"," ");
    htmlEntities.put("copy","�");
    htmlEntities.put("reg", "�");
    htmlEntities.put("euro","�");
  }

  private static final Pattern FIND_ENTITY = Pattern.compile("&(#?)(x?)(.+?);");

  public static final String unescape(String original) {
    StringBuffer out = new StringBuffer();
    Matcher M = FIND_ENTITY.matcher(original);
    while (M.find()) {
      String match = M.group(0);
      if (!M.group(2).isEmpty()) { match = new String(Character.toChars(Integer.parseInt(M.group(3), 16))); }
      else if (!M.group(1).isEmpty()) { match = new String(Character.toChars(Integer.parseInt(M.group(3), 10))); }
      else {
        String value = htmlEntities.get(M.group(3));
        if (value != null) { match = value; }
        else { System.err.println("No mapping for : " + match); }
      } 
      M.appendReplacement(out, match);
    }
    M.appendTail(out);
    return out.toString();
  }
}
