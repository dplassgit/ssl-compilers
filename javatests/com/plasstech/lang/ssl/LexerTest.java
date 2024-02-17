package com.plasstech.lang.ssl;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class LexerTest {

  @Test
  public void nextTokenEmpty() {
    Lexer lexer = new Lexer("");
    Token token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.EOF);
  }

  @Test
  public void nextTokenComment() {
    Lexer lexer = new Lexer("# nothing");
    Token token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.EOF);
  }

  @Test
  public void nextTokenIntConstant() {
    Lexer lexer = new Lexer("1");
    Token token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.CONST);
    ConstToken<Integer> constToken = (ConstToken<Integer>) token;
    assertThat(constToken.value()).isEqualTo(1);
    assertThat(constToken.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void nextTokenIntConstantEOL() {
    Lexer lexer = new Lexer("1\n");
    Token token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.CONST);
    ConstToken<Integer> constToken = (ConstToken<Integer>) token;
    assertThat(constToken.value()).isEqualTo(1);
    assertThat(constToken.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void nextTokenStringConstant() {
    Lexer lexer = new Lexer("\"hi\"");
    Token token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.CONST);
    ConstToken<String> constToken = (ConstToken<String>) token;
    assertThat(constToken.value()).isEqualTo("hi");
    assertThat(constToken.varType()).isEqualTo(VarType.STR);
  }

  @Test
  public void nextTokenIntVar() {
    Lexer lexer = new Lexer("i");
    Token token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.VAR);
    VarToken varToken = (VarToken) token;
    assertThat(varToken.name()).isEqualTo("i");
    assertThat(varToken.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void nextTokenStrVar() {
    Lexer lexer = new Lexer("s");
    Token token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.VAR);
    VarToken varToken = (VarToken) token;
    assertThat(varToken.name()).isEqualTo("s");
    assertThat(varToken.varType()).isEqualTo(VarType.STR);
  }

  @Test
  public void nextTokenKeywordVar() {
    Lexer lexer = new Lexer("if then else endif for endfor print println");
    Token token = lexer.nextToken();
    while (token.type != TokenType.EOF) {
      assertThat(token.type).isEqualTo(TokenType.KEYWORD);
      token = lexer.nextToken();
    }
  }

  @Test
  public void nextTokenSkipsComments() {
    Lexer lexer = new Lexer("\"hi\" #comment\n1");
    Token token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.CONST);
    assertThat(token.stringValue).isEqualTo("hi");
    token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.CONST);
    assertThat(token.stringValue).isEqualTo("1");
    token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.EOF);
  }

  @Test
  public void nextTokenSymbol() {
    assertSymbol("+", Symbol.PLUS);
  }

  @Test
  public void nextTokenSymbolEq() {
    assertSymbol("=", Symbol.EQ);
  }

  @Test
  public void nextTokenSymbolEqEq() {
    assertSymbol("==", Symbol.EQEQ);
  }

  @Test
  public void nextTokenSymbolNeq() {
    assertSymbol("!=", Symbol.NEQ);
  }

  @Test
  public void nextTokenSymbolLeq() {
    assertSymbol("<=", Symbol.LEQ);
  }

  @Test
  public void nextTokenAllSymbols() {
    Lexer lexer = new Lexer("+ - * / = < <= == != >= >");
    Token token = lexer.nextToken();
    while (token.type != TokenType.EOF) {
      assertThat(token.type).isEqualTo(TokenType.SYMBOL);
      System.out.print(token.stringValue + " ");
      token = lexer.nextToken();
    }
  }

  public static final String FACT = "j=1\n"
      + "n=10\n"
      + "for i = 1 to n+1\n"
      + "  j = j * i\n"
      + "endfor\n"
      + "println j\n";

  @Test
  public void fact() {
    Lexer lexer = new Lexer(FACT);
    Token token = lexer.nextToken();
    while (token.type != TokenType.EOF) {
      token = lexer.nextToken();
    }
  }

  private void assertSymbol(String input, Symbol value) {
    Lexer lexer = new Lexer(input);
    Token token = lexer.nextToken();
    assertThat(token.type).isEqualTo(TokenType.SYMBOL);
    SymbolToken symbol = (SymbolToken) token;
    assertThat(symbol.symbol()).isEqualTo(value);
  }
}
