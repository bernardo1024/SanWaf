package com.sanwaf.core;

import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

abstract class Item {
  static final String NUMERIC = "n";
  static final String NUMERIC_DELIMITED = "n{";
  static final String ALPHANUMERIC = "a";
  static final String ALPHANUMERIC_AND_MORE = "a{";
  static final String STRING = "s";
  static final String CHAR = "c";
  static final String REGEX = "r{";
  static final String JAVA = "j{";
  static final String CONSTANT = "k{";
  static final String SEP_START = "{";
  static final String SEP_END = "}";

  static final String XML_ITEMS = "items";
  static final String XML_ITEM = "item";
  static final String XML_ITEM_NAME = "name";
  static final String XML_ITEM_TYPE = "type";
  static final String XML_ITEM_MAX = "max";
  static final String XML_ITEM_MIN = "min";
  static final String XML_ITEM_MSG = "msg";
  static final String XML_ITEM_URI = "uri";
  
  static final String XML_ITEM_REQUIRED = "req";
  static final String XML_ITEM_MAX_VAL = "max-value";
  static final String XML_ITEM_MIN_VAL = "min-value";
  static final String XML_ITEM_RELATED = "related";
  static final String XML_ITEM_FORMAT = "format";
  
  String name;
  String type = null;
  int max = Integer.MAX_VALUE;
  int min = 0;
  String msg = null;
  String[] uri = null;
  
  boolean required = false;
  double maxValue;
  double minValue;
  String related;
  String format;

  Item() {
  }

  Item(String name, int max, int min, String msg, String uri) {
    this.name = name;
    this.max = max;
    this.min = min;
    this.msg = msg;
    setUri(uri);
  }

  abstract boolean inError(ServletRequest req, Shield shield, String value);
  abstract List<Point> getErrorPoints(Shield shield, String value);

  
  static Item parseItem(Xml xml) {
    return parseItem(xml, false);
  }

  static Item parseItem(Xml xml, boolean includeEnpointAttributes) {
    String name = xml.get(XML_ITEM_NAME);
    String type = xml.get(XML_ITEM_TYPE);
    String msg = xml.get(XML_ITEM_MSG);
    String uri = xml.get(XML_ITEM_URI);
    String sMax = xml.get(XML_ITEM_MAX);
    String sMin = xml.get(XML_ITEM_MIN);

    int max = Integer.MAX_VALUE;
    int min = 0;
    if (sMax.length() > 0) {
      max = Integer.parseInt(sMax);
    }
    if (sMin.length() > 0) {
      min = Integer.parseInt(sMin);
    }
    if (max == -1) {
      max = Integer.MAX_VALUE;
    }
    if (min == -1) {
      min = Integer.MAX_VALUE;
    }
    if(min < -1) {
      min = 0;
    }
    Item item = Item.getNewItem(name, type, min, max, msg, uri);
    if(includeEnpointAttributes) {
      setEndpointAttributes(xml, item);
    }
    return item;
  }
  
  static void setEndpointAttributes(Xml xml, Item item) {
    item.required =  Boolean.valueOf(xml.get(XML_ITEM_REQUIRED));
    item.related = xml.get(XML_ITEM_RELATED);
    item.format = xml.get(XML_ITEM_FORMAT);

    item.maxValue = Integer.MIN_VALUE;
    String sMaxVal = xml.get(XML_ITEM_MAX_VAL);
    if(sMaxVal.length() > 0) {
      item.maxValue = Double.valueOf(sMaxVal);
    }
    
    item.minValue = Integer.MIN_VALUE;
    String sMinVal = xml.get(XML_ITEM_MIN_VAL);
    if(sMinVal.length() > 0) {
      item.minValue = Double.valueOf(sMinVal);
    }
  }
  
  static Item getNewItem(String name, Item item) {
    item.name = name;
    return item;
  }

  static Item getNewItem(String name, String type, int min, int max, String msg, String uri) {
    Item item = null;
    String t = type.toLowerCase();
    int pos = t.indexOf(SEP_START);
    if (pos > 0) {
      t = t.substring(0, pos + SEP_START.length());
    }

    if (t.equals(NUMERIC)) {
      item = new ItemNumeric(name, max, min, msg, uri);
    } else if (t.equals(NUMERIC_DELIMITED)) {
      type = ensureTypeFormat(type);
      item = new ItemNumericDelimited(name, type, max, min, msg, uri);
    } else if (t.equals(ALPHANUMERIC)) {
      item = new ItemAlphanumeric(name, max, min, msg, uri);
    } else if (t.equals(ALPHANUMERIC_AND_MORE)) {
      type = ensureTypeFormat(type);
      item = new ItemAlphanumericAndMore(name, type, max, min, msg, uri);
    } else if (t.equals(CHAR)) {
      item = new ItemChar(name, max, min, msg, uri);
    } else if (t.equals(REGEX)) {
      type = ensureTypeFormat(type);
      item = new ItemRegex(name, type, max, min, msg, uri);
    } else if (t.equals(JAVA)) {
      type = ensureTypeFormat(type);
      item = new ItemJava(name, type, max, min, msg, uri);
    } else if (t.equals(CONSTANT)) {
      type = ensureTypeFormat(type);
      item = new ItemConstant(name, type, max, min, msg, uri);
    } else {
      item = new ItemString(name, max, min, msg, uri);
    }
    return item;
  }
  
  private static String ensureTypeFormat(String type) {
    if(!type.endsWith(SEP_END)){
      return type + SEP_END;
    }
    return type;
  }

  boolean isUriValid(ServletRequest req) {
    if (uri == null) {
      return true;
    }
    String reqUri = ((HttpServletRequest) req).getRequestURI();
    for (String u : uri) {
      if (u.equals(reqUri)) {
        return true;
      }
    }
    return false;
  }

  boolean isSizeError(String value) {
    if (value == null) {
      return min != 0;
    } else {
      return (value.length() < min || value.length() > max);
    }
  }

  String modifyErrorMsg(String errorMsg) {
    return errorMsg;
  }

  private void setUri(String uriString) {
    if (uriString != null && uriString.length() > 0) {
      uri = uriString.split(Shield.SEPARATOR);
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("name: ").append(name);
    sb.append(", type: ").append(type);
    sb.append(", max: ").append(max);
    sb.append(", min: ").append(min);
    sb.append(", msg: ").append(msg);
    sb.append(", uri: ").append(uri);
    sb.append(", required: ").append(required);
    sb.append(", max-value: ").append(maxValue);
    sb.append(", min-value: ").append(minValue);
    sb.append(", related: ").append(related);
    sb.append(", format: ").append(format);
    return sb.toString();
  }
}
