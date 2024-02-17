package com.plasstech.lang.ssl;

public abstract class TypedToken extends Token {
  private final VarType varType;

  public TypedToken(TokenType type, String value, VarType varType) {
    super(type, value);
    this.varType = varType;
  }

  public VarType varType() {
    return varType;
  }
}
