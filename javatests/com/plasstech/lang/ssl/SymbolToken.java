package com.plasstech.lang.ssl;

public class SymbolToken extends Token {

  private final Symbol symbol;

  public SymbolToken(Symbol symbol) {
    super(TokenType.SYMBOL, symbol.toString());
    this.symbol = symbol;
  }

  public Symbol symbol() {
    return symbol;
  }

}
