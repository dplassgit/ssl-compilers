package com.plasstech.lang.ssl;

public class KeywordToken extends Token {

  private final Keyword kw;

  public KeywordToken(Keyword kw) {
    super(TokenType.KEYWORD, kw.toString());
    this.kw = kw;
  }

  public Keyword keyword() {
    return kw;
  }

}
