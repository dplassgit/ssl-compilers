package com.plasstech.lang.ssl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

public class Parser {
  private final Lexer lexer;
  private final List<String> code = new LinkedList<>();
  private Token token;
  private Set<String> data = new HashSet<>();

  public Parser(String text) {
    this.lexer = new Lexer(text);
  }

  public ImmutableList<String> parse() {
    emit0("global main");
    emit0("section .text");
    emit0("main:");
    advance();
    parseStatements(ImmutableList.of(TokenType.EOF));
    emit("extern exit");
    emit("call exit\n");
    if (!data.isEmpty()) {
      emit0("section .data");
      data.forEach(entry -> {
        emit(entry);
      });
    }

    return ImmutableList.copyOf(code);
  }

  private void parseStatements(ImmutableList<TokenType> terminals) {
    while (!terminals.contains(token.type)) {
      parseStatement();
    }
  }

  private void parseStatement() {
    if (isKeyword(Keyword.PRINTLN) || isKeyword(Keyword.PRINT)) {
      parsePrint();
      return;
    }
  }

  private void parsePrint() {
    var isPrintln = isKeyword(Keyword.PRINTLN);
    advance();
    var exprType = expr();
    if (exprType == VarType.INT) {
      if (isPrintln) {
        addData("INT_NL_FMT: db '%d', 10, 0");
        emit("mov RCX, INT_NL_FMT");
      } else {
        addData("INT_FMT: db '%d', 0");
        emit("mov RCX, INT_FMT");
      }
      emit("mov EDX, EAX");
      emit("sub RSP, 0x20");
      emit("extern printf");
      emit("call printf");
      emit("add RSP, 0x20");
      return;
    }
  }

  private void addData(String entry) {
    data.add(entry);
  }

  private VarType expr() {
    var leftType = atom();
    return leftType;
  }

  private VarType atom() {
    if (token.type == TokenType.CONST) {
      var tokenType = tokenType();
      emit("mov EAX, " + token.stringValue);
      advance();
      return tokenType;
    }
    fail("Cannot parse " + token.stringValue);
    return VarType.NONE;
  }

  private void emit(String line) {
    emit0("  " + line);
  }

  private void emit0(String line) {
    code.add(line);
  }

  private void fail(String message) {
    throw new IllegalStateException(message);
  }

  private VarType tokenType() {
    TypedToken tt = (TypedToken) token;
    return tt.varType();
  }

  private boolean isKeyword(Keyword kw) {
    if (token.type != TokenType.KEYWORD) {
      return false;
    }
    KeywordToken kt = (KeywordToken) token;
    return kt.keyword() == kw;
  }

  private Token advance() {
    token = lexer.nextToken();
    return token;
  }
}
