package com.plasstech.lang.ssl;

public enum VarType {
  INT("dd"), STR("dq"), FLOAT("dq"), NONE(""), BOOL("db");

  public final String dataSize;

  private VarType(String size) {
    this.dataSize = size;
  }
}
