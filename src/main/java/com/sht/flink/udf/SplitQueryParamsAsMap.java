package com.sht.flink.udf;

import org.apache.flink.table.annotation.DataTypeHint;
import org.apache.flink.table.annotation.FunctionHint;
import org.apache.flink.table.functions.ScalarFunction;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@FunctionHint(output = @DataTypeHint("MAP<STRING, STRING>"))
public class SplitQueryParamsAsMap extends ScalarFunction {
  private static final long serialVersionUID = 1L;

  private static Pattern ampRegex = Pattern.compile("[&]");
  private static Pattern equalRegex = Pattern.compile("[=]");

  public Map<String, String> eval(String queryString) throws UnsupportedEncodingException {
    Map<String, String> result = new HashMap<>();

    String[] kvs = ampRegex.split(queryString);
    for (String kv : kvs) {
      String[] pair = equalRegex.split(kv);
      if (pair.length == 2) {
        result.put(pair[0], URLDecoder.decode(pair[1].replaceAll("\\\\x", "%"), "UTF-8"));
      }
    }

    return result;
  }
}

