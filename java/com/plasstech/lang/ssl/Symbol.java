package com.plasstech.lang.ssl;

public enum Symbol {
  PLUS("+"),
  MINUS("-"),
  MULT("*"),
  DIV("/"),
  EQEQ("=="),
  EQ("="),
  NEQ("!="),
  LEQ("<="),
  LT("<"),
  GEQ(">="),
  GT(">");

  public final String value;

  Symbol(String s) {
    this.value = s;
  }
}
