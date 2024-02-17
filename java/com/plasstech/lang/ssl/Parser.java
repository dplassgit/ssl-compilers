package com.plasstech.lang.ssl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class Parser {
  private static final Map<Symbol, String> ARITH_OPCODES =
      ImmutableMap.of(
          Symbol.PLUS, "add eax, ebx",
          Symbol.MULT, "imul eax, ebx",
          Symbol.DIV, "xchg eax, ebx\n  cdq\n  idiv ebx",
          Symbol.MINUS, "xchg eax, ebx\n  sub eax, ebx");
  private static final Map<Symbol, String> CMP_OPCODES =
      ImmutableMap.of(
          Symbol.EQEQ, "setz",
          Symbol.NEQ, "setnz",
          Symbol.LT, "setl",
          Symbol.GT, "setg",
          Symbol.GEQ, "setge",
          Symbol.LEQ, "setle");

  private final Lexer lexer;
  private final List<String> code = new LinkedList<>();
  private Token token;
  private Set<String> data = new HashSet<>();

  public Parser(String text) {
    this.lexer = new Lexer(text);
  }

  private static int counter = 0;

  private int nextInt() {
    return counter++;
  }

  private Token advance() {
    token = lexer.nextToken();
    return token;
  }

  public ImmutableList<String> parse() {
    emit0("global main");
    emit0("section .text");
    emit0("main:");
    advance();
    statements(ImmutableList.of());
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

  private void statements(ImmutableList<Keyword> terminals) {
    while (token.type != TokenType.EOF
        && !matches(terminals)) {
      statement();
    }
  }

  private boolean matches(ImmutableList<Keyword> terminals) {
    if (token.type != TokenType.KEYWORD || terminals.isEmpty()) {
      return false;
    }
    KeywordToken kt = (KeywordToken) token;
    return terminals.stream().anyMatch(kw -> kt.keyword() == kw);
  }

  private void statement() {
    if (token.type == TokenType.VAR) {
      assignment();
      return;
    }
    if (isKeyword(Keyword.PRINTLN) || isKeyword(Keyword.PRINT)) {
      parsePrint();
      return;
    }
    if (isKeyword(Keyword.IF)) {
      parseIf();
      return;
    }
    fail("Cannot parse " + token.stringValue);
  }

  private void assignment() {
    VarToken vt = (VarToken) token;
    advance();

    String varname = vt.name();
    addData(String.format("_%s: %s 0", varname, vt.varType().dataSize));

    expect(Symbol.EQ);

    VarType exprType = expr();
    checkTypes(vt.varType(), exprType);

    switch (vt.varType()) {
      case INT:
        emit(String.format("mov [_%s], EAX", varname));
        return;

      case STR:
        emit(String.format("mov [_%s], RAX", varname));
        return;

      default:
        break;
    }
    fail("Cannot parse assignment");
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
    fail("Cannot print " + exprType);
  }

  private void parseIf() {
    expect(Keyword.IF);
    VarType exprType = expr();
    checkTypes(exprType, VarType.BOOL);
    expect(Keyword.THEN);
    int elseIndex = nextInt();
    int endIfIndex = nextInt();
    emit("cmp al, 0");
    emit("jz else_" + elseIndex);
    statements(ImmutableList.of(Keyword.ELSE, Keyword.ENDIF));
    boolean hasElse = isKeyword(Keyword.ELSE);
    if (hasElse) {
      emit("jmp endif_" + endIfIndex);
    }
    emitLabel("else_" + elseIndex);
    if (hasElse) {
      advance();
      statements(ImmutableList.of(Keyword.ENDIF));
    }
    expect(Keyword.ENDIF);
    if (hasElse) {
      emitLabel("endif_" + endIfIndex);
    }
  }

  private VarType expr() {
    VarType leftType = atom();
    if (token.type == TokenType.SYMBOL) {
      emit("push rax");
      Symbol symbol = currentSymbol();
      advance();
      VarType rightType = atom();
      checkTypes(leftType, rightType);
      emit("pop rbx");
      return emitOpCodeCode(symbol, leftType);
    }
    return leftType;
  }

  private VarType emitOpCodeCode(Symbol symbol, VarType type) {
    String arith = ARITH_OPCODES.get(symbol);
    if (arith != null) {
      emit(arith);
      return type;
    }
    String cmp = CMP_OPCODES.get(symbol);
    if (cmp != null) {
      emit("cmp ebx, eax");
      emit(cmp + " al");
      return VarType.BOOL;
    }
    fail("Cannot emit opcode for " + symbol.toString());
    return VarType.NONE;
  }

  private VarType atom() {
    var tokenType = currentTokenType();
    if (token.type == TokenType.CONST) {
      switch (tokenType) {
        case INT:
          emit("mov EAX, " + token.stringValue);
          advance();
          return tokenType;

        default:
          break;
      }
    } else if (token.type == TokenType.VAR) {
      switch (tokenType) {
        case INT:
          emit(String.format("mov EAX, [_%s]", token.stringValue));
          advance();
          return tokenType;

        case STR:
          emit(String.format("mov RAX, [_%s]", token.stringValue));
          advance();
          return tokenType;

        default:
          break;
      }
    }
    fail("Cannot parse " + token.stringValue);
    return VarType.NONE;
  }

  private void expect(Symbol expected) {
    if (token.type != TokenType.SYMBOL) {
      fail("Expected " + expected + ", was " + token.stringValue);
      return;
    }
    SymbolToken st = (SymbolToken) token;
    if (st.symbol() != expected) {
      fail("Expected " + expected + ", was " + token.stringValue);
    }
    advance();
  }

  private void expect(Keyword expected) {
    if (token.type != TokenType.KEYWORD) {
      fail("Expected " + expected + ", was " + token.stringValue);
      return;
    }
    KeywordToken kw = (KeywordToken) token;
    if (kw.keyword() != expected) {
      fail("Expected " + expected + ", was " + token.stringValue);
    }
    advance();
  }

  private void checkTypes(VarType leftType, VarType rightType) {
    if (rightType != leftType) {
      fail("Cannot apply " + rightType + " to " + leftType);
    }
  }

  private void addData(String entry) {
    data.add(entry);
  }

  private void emit(String line) {
    emit0("  " + line);
  }

  private void emitLabel(String label) {
    emit0(label + ":");
  }

  private void emit0(String line) {
    code.add(line);
  }

  private void fail(String message) {
    throw new IllegalStateException(message);
  }

  private Symbol currentSymbol() {
    SymbolToken st = (SymbolToken) token;
    return st.symbol();
  }

  private VarType currentTokenType() {
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
}
