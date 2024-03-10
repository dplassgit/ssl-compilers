package com.plasstech.lang.ssl

import org.scalatest.funsuite.AnyFunSuite

class LexerTest extends AnyFunSuite:

  test("empty") {
    val lexer = new Lexer("")
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.EndOfFile)
  }

  test("whitespace") {
    val lexer = new Lexer("  \n\r\t ")
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.EndOfFile)
  }

  test("comment") {
    val lexer = new Lexer("#")
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.EndOfFile)
  }

  test("comment with whitespce") {
    val lexer = new Lexer("  \n#\r\t ")
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.EndOfFile)
  }

  test("int constant") {
    val lexer = new Lexer("3")
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "3")
    assert(t.varType() == VarType.VarTypeInt)
  }

  test("multidigit int constant") {
    val lexer = new Lexer("314")
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "314")
    assert(t.varType() == VarType.VarTypeInt)
  }

  test("multiple int constants") {
    val lexer = new Lexer("314 628")
    var t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "314")
    assert(t.varType() == VarType.VarTypeInt)
    t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "628")
    assert(t.varType() == VarType.VarTypeInt)
  }

  test("float constant") {
    val lexer = new Lexer("3.0")
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "3.0")
    assert(t.varType() == VarType.VarTypeFloat)
  }

  test("longer float constant") {
    val lexer = new Lexer("314.159")
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "314.159")
    assert(t.varType() == VarType.VarTypeFloat)
  }

  test("string constant") {
    val lexer = new Lexer("\"hi\"")
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "hi")
    assert(t.varType() == VarType.VarTypeString)
  }

  test("multiline string") {
    val lexer = new Lexer("\"hi\nthere\"")
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "hi\nthere")
    assert(t.varType() == VarType.VarTypeString)
  }

  test("unclosed string constant") {
    val lexer = new Lexer("\"hi")
    assertThrows[Exception]{lexer.nextToken()}
  }

  test("comment between tokens") {
    val lexer = new Lexer("314 #comment\n 628")
    var t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "314")
    t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Constant)
    assert(t.value() == "628")
  }

  test("variables") {
    val lexer = new Lexer("a i s")
    var t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Variable)
    assert(t.value() == "a")
    assert(t.varType() == VarType.VarTypeFloat)
    t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Variable)
    assert(t.value() == "i")
    assert(t.varType() == VarType.VarTypeInt)
    t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Variable)
    assert(t.value() == "s")
    assert(t.varType() == VarType.VarTypeString)
  }

  test("upper variables") {
    val lexer = new Lexer("A I S")
    var t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Variable)
    assert(t.value() == "A")
    assert(t.varType() == VarType.VarTypeFloat)
    t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Variable)
    assert(t.value() == "I")
    assert(t.varType() == VarType.VarTypeInt)
    t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Variable)
    assert(t.value() == "S")
    assert(t.varType() == VarType.VarTypeString)
  }

  test("keyword") {
    val lexer = new Lexer("println")
    var t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Keyword)
    assert(t.keyword() == KeywordType.Println)
  }

  test("all keywords") {
    // All keywords except "NoKeyword"
    val allKeywords = KeywordType.values.filter(_ != KeywordType.NoKeyword)
    val program = allKeywords.map(_.toString).mkString(" ")
    val lexer = new Lexer(program)

    for kw <- allKeywords do
      val t = lexer.nextToken()
      assert(t.tokenType() == TokenType.Keyword)
      assert(t.keyword() == kw)

    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.EndOfFile)
  }

  test("keywords are case insensitive") {
    val lexer = new Lexer("pRINTln If THEN")
    var t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Keyword)
    assert(t.keyword() == KeywordType.Println)
    t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Keyword)
    assert(t.keyword() == KeywordType.If)
    t = lexer.nextToken()
    assert(t.tokenType() == TokenType.Keyword)
    assert(t.keyword() == KeywordType.Then)
  }

  test("invalid keywords are invalid") {
    val lexer = new Lexer("nope")
    assertThrows[Exception]{lexer.nextToken()}
  }

  test("all symbols") {
    val allSymbols = SymbolType.values.filter(_ != SymbolType.NoSymbol)
    val program = allSymbols.map(_.text()).mkString(" ")

    val lexer = new Lexer(program)

    for st <- allSymbols do
      val t = lexer.nextToken()
      assert(t.tokenType() == TokenType.Symbol)
      assert(t.symbolType() == st)
    val t = lexer.nextToken()
    assert(t.tokenType() == TokenType.EndOfFile)
  }

  test("invalid symbols are invalid") {
    val lexer = new Lexer("%")
    assertThrows[Exception]{lexer.nextToken()}
  }

end LexerTest
