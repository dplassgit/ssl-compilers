package com.plasstech.lang.ssl

import org.scalatest.funsuite.AnyFunSuite

class TokenTest extends AnyFunSuite:

  test("creating eof token returns eof") {
    val t = new Token(TokenType.EndOfFile)
    assert(t.tokenType() == TokenType.EndOfFile)
  }

  test("creating a keyword sets the token type") {
    val t = new Token(KeywordType.Print)
    assert(t.tokenType() == TokenType.Keyword)
    assert(t.keyword() == KeywordType.Print)
  }

  test("creating a symbol sets the token type") {
    val t = new Token(SymbolType.EqEq)
    assert(t.tokenType() == TokenType.Symbol)
    assert(t.symbolType() == SymbolType.EqEq)
  }

  test("creating a variable sets the token type") {
    val t = newVariable("name", VarType.VarTypeInt)
    assert(t.tokenType() == TokenType.Variable)
    assert(t.value() == "name")
    assert(t.varType() == VarType.VarTypeInt)
  }

  test("creating a constant sets the token type") {
    val t = newConstant("123.0", VarType.VarTypeFloat)
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "123.0")
    assert(t.varType() == VarType.VarTypeFloat)
  }

  test("creating a constant string sets the token type") {
    val t = newConstant("123.0", VarType.VarTypeString)
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "123.0")
    assert(t.varType() == VarType.VarTypeString)
  }
end TokenTest

