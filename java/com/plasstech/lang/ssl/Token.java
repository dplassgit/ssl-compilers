package com.plasstech.lang.ssl;

public class Token {
  public final TokenType type;
  public final String stringValue;

  public Token(TokenType type, String value) {
    this.type = type;
    this.stringValue = value;
  }

}
