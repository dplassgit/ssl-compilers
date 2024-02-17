package com.plasstech.lang.ssl;

public class VarToken extends Token {
  private final VarType varType;

  public VarToken(String name, VarType varType) {
    super(TokenType.VAR, name);
    this.varType = varType;
  }

  public VarType varType() {
    return varType;
  }

  public String name() {
    return stringValue;
  }

}
