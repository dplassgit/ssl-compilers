package com.plasstech.lang.ssl;

import com.google.common.collect.ImmutableSet;

public class Lexer {
  private static final ImmutableSet<Character> EQUALS_CAN_FOLLOW =
      ImmutableSet.of('>', '!', '<', '=');

  private final String text;

  private int loc;
  private char cc; // current character

  public Lexer(String text) {
    this.text = text;
    this.loc = 0;
    this.cc = 0;
    this.advance();
  }

  private void advance() {
    if (loc < text.length()) {
      cc = text.charAt(loc);
    } else {
      // Indicates no more characters
      cc = 0;
    }
    loc++;
  }

  public Token nextToken() {
    // skip unwanted whitespace
    while (true) {
      while (cc == ' ' || cc == '\n' || cc == '\t' || cc == '\r') {
        advance();
      }
      if (cc != '#') { // # comment
        break;
      }
      while (cc != '\n' && cc != '\r' && cc != 0) {
        advance();
      }
    }

    if (cc == 0) {
      return new Token(TokenType.EOF, "");
    }

    if (Character.isDigit(cc)) {
      return makeNumber();
    } else if (Character.isAlphabetic(cc)) {
      return makeText();
    } else if (cc == '"') {
      return makeString();
    }
    return makeSymbol();
  }

  private Token makeSymbol() {
    var first = cc;
    String symbolString = String.valueOf(first);
    advance(); // eat the first
    if (EQUALS_CAN_FOLLOW.contains(first) && cc == '=') {
      symbolString += cc;
      advance(); // eat the second
    }
    for (Symbol symbolEnum : Symbol.values()) {
      if (symbolEnum.value.equals(symbolString)) {
        return new SymbolToken(symbolEnum);
      }
    }
    fail("Unknown symbol " + symbolString);
    return null;
  }

  private Token makeString() {
    advance(); // eat the "
    String value = "";
    while (cc != '"' && cc != 0) {
      value = value + cc;
      advance();
    }
    if (cc == '"') {
      advance();
    } else {
      fail("Expected closing double quote, found EOF");
    }
    return new ConstToken<String>(value, VarType.STR);
  }

  private void fail(String message) {
    throw new IllegalStateException(message);
  }

  private Token makeText() {
    var first = cc;
    advance();
    if (!Character.isAlphabetic(cc)) {
      String var = String.valueOf(first);
      if (first >= 'a' && first <= 'h') {
        return new VarToken(var, VarType.FLOAT);
      } else if (first >= 'i' && first <= 'n') {
        return new VarToken(var, VarType.INT);
      } else {
        return new VarToken(var, VarType.STR);
      }
    }
    String keyword = String.valueOf(first);
    try {
      while (Character.isAlphabetic(cc)) {
        keyword += cc;
        advance();
      }
      // look up the keyword
      var kw = Keyword.valueOf(keyword.toUpperCase());
      return new KeywordToken(kw);
    } catch (IllegalArgumentException e) {
      fail("Unknown keyword " + keyword);
      return null;
    }
  }

  private Token makeNumber() {
    var first = cc;
    advance();
    String num = String.valueOf(first);
    while (Character.isDigit(cc)) {
      num += cc;
      advance();
    }
    if (cc == '.') {
      num += cc;
      advance();
      while (Character.isDigit(cc)) {
        num += cc;
        advance();
      }
      return new ConstToken<Float>(Float.parseFloat(num), VarType.FLOAT);
    }
    return new ConstToken<Integer>(Integer.parseInt(num), VarType.INT);
  }
}
