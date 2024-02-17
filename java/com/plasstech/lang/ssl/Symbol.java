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
  LE("<"),
  GEQ(">="),
  GE(">");

  public final String value;

  Symbol(String s) {
    this.value = s;
  }
}
