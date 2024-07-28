package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;

public class RosettaUtils {

  public static final String[] RESERVED_FUNCTION_NAMES = new String[] {"toString", "valueOf"};
  public static final String[] RESERVED_WORDS =
      new String[] {
        "and",
        "break",
        "do",
        "else",
        "elseif",
        "end",
        "false",
        "for",
        "function",
        "if",
        "in",
        "local",
        "nil",
        "not",
        "or",
        "repeat",
        "return",
        "then",
        "true",
        "until",
        "while",

        // NOTE: This is a technical issue involving YAML interpreting
        //       this as a BOOLEAN not a STRING value.
        "on",
        "off",
        "yes",
        "no",
      };

  public static String formatName(@NotNull String name) {
    for (String reservedWord : RESERVED_WORDS) {
      if (name.toLowerCase().equals(reservedWord)) return "__" + name + "__";
    }
    for (String reservedFunctionName : RESERVED_FUNCTION_NAMES) {
      if (name.equals(reservedFunctionName)) return "__" + name + "__";
    }
    return name;
  }
}
