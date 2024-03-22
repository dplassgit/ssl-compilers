package com.plasstech.lang.ssl

import org.junit.Test
import com.google.common.truth.Truth.assertThat

internal class LexerTest {
  @Test
  fun nextTokenEmpty() {
    val lexer = Lexer("")
    val token = lexer.nextToken()
    assertThat(token.type).isEqualTo(TokenType.EndOfFile)
  }

  @Test
  fun nextTokenComment() {
    val lexer = Lexer("# nothing")
    val token = lexer.nextToken()
    assertThat(token.type).isEqualTo(TokenType.EndOfFile)
  }

  @Test
  fun nextTokenint() {
    val lexer = Lexer("1\n")
    val token = lexer.nextToken()
    assertThat(token.type).isEqualTo(TokenType.Constant)
    assertThat(token.varType()).isEqualTo(VarType.Int)
    assertThat(token.value).isEqualTo("1")
    val eofToken = lexer.nextToken()
    assertThat(eofToken.type).isEqualTo(TokenType.EndOfFile)
  }

  private fun testVariables(vars: List<String>) {
    val lexer = Lexer(vars.joinToString(" "))
    val expected = listOf(VarType.Float, VarType.Int, VarType.Str, VarType.Str)
    var index = 0
    for (varType in expected) {
      val token = lexer.nextToken()
      assertThat(token.type).isEqualTo(TokenType.Variable)
      assertThat(token.varType()).isEqualTo(varType)
      assertThat(token.value).isEqualTo(vars.get(index))
      index++
    }
  }

  @Test
  fun variables() {
    testVariables(listOf("a", "i", "s", "z"))
  }

  @Test
  fun upperVariables() {
    testVariables(listOf("A", "I", "S", "Z"))
  }

  private fun testKeywords(input: String) {
    val lexer = Lexer(input)
    val expected = listOf(
      KeywordType.IF,
      KeywordType.THEN,
      KeywordType.ELSE,
      KeywordType.ENDIF,
      KeywordType.FOR,
      KeywordType.TO,
      KeywordType.STEP,
      KeywordType.ENDFOR,
      KeywordType.PRINTLN,
      KeywordType.PRINT)
    for (keyword in expected) {
      val token = lexer.nextToken()
      assertThat(token.type).isEqualTo(TokenType.Keyword)
      assertThat(token.keyword()).isEqualTo(keyword)
    }
  }


  @Test
  fun keywords() {
    testKeywords("if then else endif for to step endfor println print");
  }

  @Test
  fun upperKeywords() {
    testKeywords("IF THEN ELSE ENDIF FOR TO STEP ENDFOR PRINTLN PRINT");
  }

  private fun testSymbols(input: String) {
    val lexer = Lexer(input)
    val expected = listOf(
      SymbolType.Lt,
      SymbolType.Gt,
      SymbolType.Eq,
      SymbolType.Geq,
      SymbolType.Leq,
      SymbolType.Neq,
      SymbolType.EqEq,
      SymbolType.Mult,
      SymbolType.Plus,
      SymbolType.Div,
      SymbolType.Minus)

    for (symbol in expected) {
      val token = lexer.nextToken()
      assertThat(token.type).isEqualTo(TokenType.Symbol)
      assertThat(token.symbol()).isEqualTo(symbol)
    }
  }


  @Test
  fun nextTokenAllSymbolsSpaceSeparated() {
    testSymbols("<> = >= <= != == * + / -")
  }

  @Test
  fun nextTokenAllSymbols() {
    testSymbols("<> =>=<=!===*+/-")
  }
}
