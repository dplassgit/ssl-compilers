package com.plasstech.lang.ssl;

public class ConstToken<T> extends TypedToken {
  private final T value;

  public ConstToken(T value, VarType varType) {
    super(TokenType.CONST, value.toString(), varType);
    this.value = value;
  }

  public T value() {
    return value;
  }
}
