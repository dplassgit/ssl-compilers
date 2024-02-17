package com.plasstech.lang.ssl;

public class ConstToken<T> extends Token {
  private final VarType varType;
  private final T value;

  public ConstToken(T value, VarType varType) {
    super(TokenType.CONST, value.toString());
    this.varType = varType;
    this.value = value;
  }

  public VarType varType() {
    return varType;
  }

  public T value() {
    return value;
  }
}
