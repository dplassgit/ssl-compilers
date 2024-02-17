package com.plasstech.lang.ssl;

import java.util.HashMap;
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
  // Maps from value to name
  private Map<String, String> stringTable = new HashMap<>();

  public Parser(String text) {
    this.lexer = new Lexer(text);
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

  private static int counter = 0;

  private int nextInt() {
    return counter++;
  }

  private String nextLabel(String prefix) {
    return String.format("%s_%d", prefix, nextInt());
  }

  private Token advance() {
    token = lexer.nextToken();
    return token;
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
    if (isKeyword(Keyword.FOR)) {
      parseFor();
      return;
    }
    fail("Cannot parse " + token.stringValue);
  }

  private void parseFor() {
    expect(Keyword.FOR);
    if (token.type != TokenType.VAR) {
      fail("Expected VAR");
      return;
    }
    if (currentTokenType() != VarType.INT) {
      fail("FOR variable must be integer");
      return;
    }
    VarToken vt = (VarToken) token;
    String varName = vt.name();
    addData(String.format("_%s: dd 0", varName));
    advance();

    expect(Symbol.EQ);
    VarType startType = expr();
    if (startType != VarType.INT) {
      fail("FOR start condition must be integer");
      return;
    }
    emit("mov [_%s], EAX", varName);
    expect(Keyword.TO);

    String startForLabel = nextLabel("startFor");
    String endForLabel = nextLabel("endFor");
    emitLabel(startForLabel);
    VarType endType = expr();
    if (endType != VarType.INT) {
      fail("FOR end condition must be integer");
      return;
    }
    emit("cmp [_%s], EAX", varName);
    emit("jge %s", endForLabel);

    statements(ImmutableList.of(Keyword.ENDFOR));
    emit("inc DWORD [_%s]", varName);
    emit("jmp %s", startForLabel);
    emitLabel(endForLabel);
    expect(Keyword.ENDFOR);
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
        emit("mov [_%s], EAX", varname);
        return;

      case STR:
        emit("mov [_%s], RAX", varname);
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
    switch (exprType) {
      case INT:
        addData("INT_FMT: db '%d', 0");
        emit("mov RCX, INT_FMT");
        emit("mov EDX, EAX");
        break;

      case STR:
        emit("mov RCX, RAX");
        break;

      case BOOL:
        addData("TRUE: db 'true', 0");
        addData("FALSE: db 'false', 0");
        emit("cmp al, 1");
        emit("mov RCX, FALSE");
        emit("mov RDX, TRUE");
        emit("cmovz RCX, RDX");
        break;

      default:
        fail("Cannot print " + exprType);
        break;
    }
    emit("sub RSP, 0x20");
    emit("extern printf");
    emit("call printf");
    if (isPrintln) {
      emit("extern putchar");
      emit("mov rcx, 10");
      emit("call putchar");
    }
    emit("add RSP, 0x20");
  }

  private void parseIf() {
    expect(Keyword.IF);
    VarType exprType = expr();
    checkTypes(exprType, VarType.BOOL);
    expect(Keyword.THEN);
    String elseLabel = nextLabel("else");
    String endIfLabel = nextLabel("endIf");
    emit("cmp al, 0");
    emit("jz " + elseLabel);
    statements(ImmutableList.of(Keyword.ELSE, Keyword.ENDIF));
    boolean hasElse = isKeyword(Keyword.ELSE);
    if (hasElse) {
      emit("jmp " + endIfLabel);
    }
    emitLabel(elseLabel);
    if (hasElse) {
      advance();
      statements(ImmutableList.of(Keyword.ENDIF));
    }
    expect(Keyword.ENDIF);
    if (hasElse) {
      emitLabel(endIfLabel);
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

        case STR:
          var value = token.stringValue;
          var name = addStringConstant(value);
          emit("mov RAX, " + name);
          advance();
          return tokenType;

        default:
          break;
      }
    }
    if (token.type == TokenType.VAR) {
      switch (tokenType) {
        case INT:
          emit("mov EAX, [_%s]", token.stringValue);
          advance();
          return tokenType;

        case STR:
          emit("mov RAX, [_%s]", token.stringValue);
          advance();
          return tokenType;

        default:
          break;
      }
    }
    fail("Cannot parse " + token.stringValue);
    return VarType.NONE;
  }

  private Object addStringConstant(String value) {
    String name = stringTable.get(value);
    if (name != null) {
      return name;
    }
    name = nextLabel("CONST");
    stringTable.put(value, name);
    addData(String.format("%s: db \"%s\", 0", name, value));
    return name;
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

  private void emit(String format, Object... params) {
    emit(String.format(format, params));
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
