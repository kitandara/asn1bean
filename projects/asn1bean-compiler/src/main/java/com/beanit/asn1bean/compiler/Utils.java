package com.beanit.asn1bean.compiler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class Utils {

  public static final Set<String> reservedKeywords =
      Collections.unmodifiableSet(
          new TreeSet<>(
              Arrays.asList(
                  "public",
                  "private",
                  "protected",
                  "final",
                  "void",
                  "int",
                  "short",
                  "float",
                  "double",
                  "long",
                  "byte",
                  "char",
                  "String",
                  "throw",
                  "throws",
                  "new",
                  "static",
                  "volatile",
                  "if",
                  "else",
                  "for",
                  "switch",
                  "case",
                  "enum",
                  "this",
                  "super",
                  "boolean",
                  "class",
                  "abstract",
                  "package",
                  "import",
                  "null",
                  "code",
                  "getClass",
                  "setClass")));

  static String cleanUpName(String name) {

    name = replaceCharByCamelCase(name, '-');
    name = replaceCharByCamelCase(name, '_');

    return sanitize(name);
  }

  static String sanitize(String name) {
    if (name.isEmpty()) return name;
    String result = replaceCharByCamelCase(name, '.');
    if (Character.isDigit(result.charAt(0))) {
      result = "_" + result;
    }
    if (reservedKeywords.contains(result)) {
      result += "_";
    }
    return result;
  }

  private static String replaceCharByCamelCase(String name, char charToBeReplaced) {
    StringBuilder nameSb = new StringBuilder(name);

    int index = name.indexOf(charToBeReplaced);
    while (index != -1 && index != (name.length() - 1)) {
      if (!Character.isUpperCase(name.charAt(index + 1))) {
        nameSb.setCharAt(index + 1, Character.toUpperCase(name.charAt(index + 1)));
      }
      index = name.indexOf(charToBeReplaced, index + 1);
    }

    name = nameSb.toString();
    name = name.replace("" + charToBeReplaced, "");

    return name;
  }

  public static String lastPartOfName(String className)
  {
    String[] parts = className.split("\\.",-1);
    return parts.length > 0 ? parts[parts.length-1] : className;
  }

  public static String lastPartOfPackageName(String moduleName)
  {
    String[] moduleParts = moduleName.split("-", -1);
    int l = moduleParts.length;
    return l > 0 ? Utils.sanitize(moduleParts[l-1].toLowerCase()) : "anon";
  }
  public static String moduleToPackageName(String moduleName, String sep) {
    String[] moduleParts = moduleName.split("-", -1);
    StringBuilder packageName = new StringBuilder();
    for (String part : moduleParts) {
      if (packageName.length() > 0) {
        packageName.append(sep);
      }
      packageName.append(Utils.sanitize(part.toLowerCase()));
    }
    return packageName.toString();
  }
}
