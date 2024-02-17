package com.plasstech.lang.ssl;

public class VarToken extends TypedToken {
  public VarToken(String name, VarType varType) {
    super(TokenType.VAR, name, varType);
  }

  public String name() {
    return stringValue;
  }
}
