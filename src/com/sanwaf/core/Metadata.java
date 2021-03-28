package com.sanwaf.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Metadata {
  static final String XML_METADATA = "metadata";
  static final String XML_SECURED = "secured";
  static final String XML_PARAMETERS = "parameters";
  static final String XML_HEADERS = "headers";
  static final String XML_COOKIES = "cookies";
  static final String INDEX_PARM_MARKER = "  ";
  static final String STAR = "*";

  boolean enabled = false;
  boolean caseSensitive = true;
  boolean endpointIsStrict = false;
  boolean endpointIsStrictAllowLess = false;
  Map<String, Item> items = new HashMap<>();
  Map<String, List<String>> index = new HashMap<>();

  Metadata(Xml xml, String type) {
    load(xml, type);
  }

  Metadata(String itemsString, boolean caseSensitive, boolean includeEndpointAttributes, String endpointIsStrict) {
    load(itemsString, caseSensitive, includeEndpointAttributes);

    if(endpointIsStrict.length() > 0 && !"false".equalsIgnoreCase(endpointIsStrict)) {
      this.endpointIsStrict = true;
      if(endpointIsStrict.contains("<")) {
        this.endpointIsStrictAllowLess = true;
      }
    }
  }

  String getFromIndex(String key) {
    List<String> list = index.get(key.substring(0, 1));
    if (list == null) {
      return null;
    }

    for (String s : list) {
      int last = 0;
      while (true) {
        if (s.length() != 2) {
          return resolveStarAtEndOfWord(key, list);
        }
        int start = key.indexOf(s.charAt(0), last);
        if (start <= 0) {
          break;
        }
        int end = key.indexOf(s.charAt(1), start + 1);
        last = end + 1;
        key = key.substring(0, start + 1) + key.substring(end, key.length());
      }
    }
    return key;
  }

  private String resolveStarAtEndOfWord(String key, List<String> list) {
    String k2 = stripEosNumbers(key);
    if (list.contains(INDEX_PARM_MARKER + k2)) {
      return k2;
    }
    return null;
  }

  void load(Xml xml, String type) {
    initA2Zindex(index);

    String metadataBlock = xml.get(XML_METADATA);
    Xml metadataBlockXml = new Xml(metadataBlock);
    String securedBlock = metadataBlockXml.get(XML_SECURED);
    Xml securedBlockXml = new Xml(securedBlock);

    String enabledViewBlock = metadataBlockXml.get(Shield.XML_ENABLED);
    Xml enabledViewdBlockXml = new Xml(enabledViewBlock);
    enabled = Boolean.parseBoolean(enabledViewdBlockXml.get(type));

    String caseBlock = metadataBlockXml.get(Shield.XML_CASE_SENSITIVE);
    Xml caseBlockXml = new Xml(caseBlock);
    caseSensitive = Boolean.parseBoolean(caseBlockXml.get(type));

    String subBlock = securedBlockXml.get(type);
    Xml subBlockXml = new Xml(subBlock);
    String[] xmlItems = subBlockXml.getAll(Item.XML_ITEM);
    for (String itemString : xmlItems) {
      loadItem(itemString, false);
    }
  }

  void load(String itemsString, boolean caseSensitive, boolean includeEndpointAttributes) {
    initA2Zindex(index);
    enabled = true;
    this.caseSensitive = caseSensitive;

    Xml itemsXml = new Xml(itemsString);
    String[] xmlItems = itemsXml.getAll(Item.XML_ITEM);
    for (String itemString : xmlItems) {
      loadItem(itemString, includeEndpointAttributes);
    }
  }

  private void loadItem(String itemString, boolean includeEnpointAttributes) {
    Xml xml = new Xml(itemString);
    Item item = Item.parseItem(xml, includeEnpointAttributes);
    String namesString = xml.get(Item.XML_ITEM_NAME);
    
    if(namesString.contains(Shield.SEPARATOR)) {
      String[] names = namesString.split(Shield.SEPARATOR);
      for (String name : names) {
        name = refineName(name, index);
        if (name == null) {
          continue;
        }
        if (!caseSensitive) {
          name = name.toLowerCase();
        }
        items.put(name, Item.getNewItem(name, item));
      }
    }
    else {
      item.name = refineName(item.name, index);
      if (item.name != null) {
        if (!caseSensitive) {
          item.name = item.name.toLowerCase();
        }
        items.put(item.name, item);
      }
    }
  }
  
  static void initA2Zindex(Map<String, List<String>> map) {
    for (char ch = 'a'; ch <= 'z'; ++ch) {
      map.put(String.valueOf(ch), null);
    }
  }

  static String refineName(String name, Map<String, List<String>> map) {
    int last = 0;
    while (true) {
      int starPos = name.indexOf(STAR, last);
      if (starPos < 0) {
        return name;
      }
      if (starPos == 0) {
        return null;
      }
      String f = name.substring(starPos - 1, starPos);
      String markerChars;

      if (starPos == name.length() - 1) {
        markerChars = INDEX_PARM_MARKER + name.substring(0, name.length() - 1);
      } else {
        markerChars = f + name.substring(starPos + 1, starPos + 2);
        if (!isNotAlphanumeric(markerChars)) {
          return null;
        }
      }
      String firstCharOfKey = name.substring(0, 1);
      List<String> chars = map.computeIfAbsent(firstCharOfKey, k -> new ArrayList<>());
      if (!chars.contains(markerChars)) {
        chars.add(markerChars);
      }
      name = name.substring(0, starPos) + name.substring(starPos + 1, name.length());
    }
  }

  static String stripEosNumbers(final String s) {
    int i = s.length() - 1;
    while (i > 0) {
      char c = s.charAt(i);
      int v = c - '0';
      if (v >= 0 && v <= 9) {
        i--;
        continue;
      }
      return s.substring(0, i + 1);
    }
    return s;
  }

  static boolean isNotAlphanumeric(String s) {
    char[] chars = s.toCharArray();
    for (char c : chars) {
      if (!(c < 0x30 || (c >= 0x3a && c <= 0x40) || (c > 0x5a && c <= 0x60) || c > 0x7a)) {
        return false;
      }
    }
    return true;
  }
  
  static boolean isFormatError(String format, String value) {
    int formatLen = format.length();
    if (value.length() != formatLen) {
      return true;
    }

    for (int i = 0; i < value.length(); i++) {
      char f = format.charAt(i);
      char c = value.charAt(i);
      if ((f == '#' && c >= '0' && c <= '9') || (f == 'A' && c >= 'A' && c <= 'Z') || (f == 'a' && c >= 'a' && c <= 'z')) {
        continue;
      }
      if (c != f) {
        return true;
      }
    }
    return false;
  }

  static String jsonEncode(String s) {
    if (s == null) {
      return "";
    } else {
      s = s.replace("\\", "\\\\");
      s = s.replace("\"", "\\\"");
      return s.replace("/", "\\/");
    }
  }
}
