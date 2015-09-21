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

    htmlEntities.put("aacute","á"); htmlEntities.put("Aacute","Á");
    htmlEntities.put("agrave","à"); htmlEntities.put("Agrave","À");
    htmlEntities.put("acirc", "â"); htmlEntities.put("Acirc", "Â");
    htmlEntities.put("auml",  "ä"); htmlEntities.put("Auml",  "Ä");
    htmlEntities.put("aring", "å"); htmlEntities.put("Aring", "Å");
    htmlEntities.put("aelig", "æ"); htmlEntities.put("AElig", "Æ");
    htmlEntities.put("ccedil","ç"); htmlEntities.put("Ccedil","Ç");
    htmlEntities.put("eacute","é"); htmlEntities.put("Eacute","É");
    htmlEntities.put("egrave","è"); htmlEntities.put("Egrave","È");
    htmlEntities.put("ecirc", "ê"); htmlEntities.put("Ecirc", "Ê");
    htmlEntities.put("euml",  "ë"); htmlEntities.put("Euml",  "Ë");
    htmlEntities.put("iacute","í"); htmlEntities.put("Iacute","Í");
    htmlEntities.put("icirc", "î"); htmlEntities.put("Icirc", "Î");
    htmlEntities.put("iuml",  "ï"); htmlEntities.put("Iuml",  "Ï");
    htmlEntities.put("ntilde","ñ"); htmlEntities.put("Ntilde","Ñ");
    htmlEntities.put("oacute","ó"); htmlEntities.put("Oacute","Ó");
    htmlEntities.put("ocirc", "ô"); htmlEntities.put("Ocirc", "Ô");
    htmlEntities.put("ouml",  "ö"); htmlEntities.put("Ouml",  "Ö");
    htmlEntities.put("oslash","ø"); htmlEntities.put("Oslash","Ø");
    htmlEntities.put("szlig", "ß");
    htmlEntities.put("uacute","ú"); htmlEntities.put("Uacute","Ú");
    htmlEntities.put("ugrave","ù"); htmlEntities.put("Ugrave","Ù");
    htmlEntities.put("ucirc", "û"); htmlEntities.put("Ucirc", "Û");
    htmlEntities.put("uuml",  "ü"); htmlEntities.put("Uuml",  "Ü");
    
    htmlEntities.put("nbsp"," ");
    htmlEntities.put("copy","©");
    htmlEntities.put("reg", "®");
    htmlEntities.put("euro","€");
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
