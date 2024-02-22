#include "lexer.h"
#include "parser.h"
#include "token.h"

#include <iostream>
#include <map>
#include <string>

using namespace std;

void fail(string msg) {
  cerr << msg << endl;
  exit(-1);
}

const string NOT_FOUND = " _ NOT _ FOUND _ ";

template<typename T> string lookup(map<T, string> table, T key) {
  auto iter = table.find(key);
  if (iter != table.end()) {
    return iter->second;
  }
  return NOT_FOUND;
}


vector<string> Parser::parse() {
  emit0("global main");
  emit0("section .text");
  emit0("main:");
  statements();
  emit("extern exit");
  emit("call exit\n");

  if (!data.empty()) {
    emit0("section .data");
    for (auto iter = data.begin(); iter != data.end(); iter++) {
      emit(*iter);
    }

  }
  return code;
}

void Parser::statements() {
  while (token->type() != END_OF_FILE) {
    statement();
  }
}

void Parser::statement() {
  if (token->type() == VAR) {
    assignment();
    return;
  }
  if (token->isKeyword(PRINTLN) || token->isKeyword(PRINT)) {
    parsePrint();
    return;
  }
  if (token->isKeyword(IF)) {
    parseIf();
    return;
  }
  if (token->isKeyword(FOR)) {
    parseFor();
    return;
  }
  fail("Skipping unknown token " + token->value());
  advance();
}

void Parser::assignment() {
  VarType varType = token->varType();
  string name = token->value();
  addData(name, varType);
  advance();

  expect(EQ);

  VarType exprType = expr();
  checkTypes(varType, exprType);
  switch (varType) {
    case INT:
      emit("mov [_" + name + "], EAX");
      break;
    case STR:
      emit("mov [_" + name + "], RAX");
      break;
    case FLOAT:
      emit("movq [_" + name + "], XMM0");
      break;
  }
}

void Parser::parsePrint() {
  bool isPrintln = token->isKeyword(PRINTLN);
  advance();
  VarType exprType = expr();
  switch (exprType) {
    case INT:
      addData("INT_FMT: db '%d', 0");
      emit("mov RCX, INT_FMT");
      emit("mov EDX, EAX");
      break;

    case STR:
      emit("mov RCX, RAX");
      break;

    case BOOL_TYPE:
      addData("TRUE: db 'true', 0");
      addData("FALSE: db 'false', 0");
      emit("cmp al, 1");
      emit("mov RCX, FALSE");
      emit("mov RDX, TRUE");
      emit("cmovz RCX, RDX");
      break;

    case FLOAT:
      addData("FLOAT_FMT: db '%.16g', 0");
      emit("mov RCX, FLOAT_FMT");
      emit("movq RDX, XMM0");
      break;

    default:
      fail("Cannot print token " + token->value());
      break;
  }
  emit("sub RSP, 0x20");
  emit("extern printf");
  emit("call printf");
  if (isPrintln) {
    emit("extern putchar");
    emit("mov RCX, 10");
    emit("call putchar");
  }
  emit("add RSP, 0x20");
}

void Parser::parseIf() {
  expect(IF);
  VarType condType = expr();
  checkTypes(condType, BOOL_TYPE);
  string elseLabel = nextLabel("else");
  string endIfLabel = nextLabel("endif");
  emit("cmp AL, 0x01");
  emit("jne " + elseLabel);

  expect(THEN);
  while (!token->isKeyword(ENDIF) && !token->isKeyword(ELSE)
      && token->type() != END_OF_FILE) {
    statement();
  }
  if (token->type() == END_OF_FILE) {
    fail("Expected ELSE or ENDIF, found EOF");
  }

  bool hasElse = token->isKeyword(ELSE);
  if (hasElse) {
    // only have to jump to end if we are at "else" in the parse
    emit("jmp " + endIfLabel);
  }
  emitLabel(elseLabel);
  if (hasElse) {
    advance();
    while (!token->isKeyword(ENDIF) && token->type() != END_OF_FILE) {
      statement();
    }
  }
  expect(ENDIF);
  if (hasElse) {
    emitLabel(endIfLabel);
  }
}

void Parser::parseFor() {
  expect(FOR);
  if (token->type() != VAR) {
    fail("Expected VARIABLE, found " + token->value());
    return;
  }
  VarType varType = token->varType();
  if (varType != INT) {
    fail("FOR variable must be integer");
    return;
  }
  string varName = token->value();
  addData(varName, varType);
  string varRef = "[_" + varName + "]";
  advance();

  expect(EQ);

  VarType fromType = expr();
  if (fromType != INT) {
    fail("FOR 'from' expression must be integer");
    return;
  }
  // Set variable to "from"
  emit("mov " + varRef + ", EAX");
  expect(TO);

  string forLabel = nextLabel("for");
  string endForLabel = nextLabel("endfor");
  emitLabel(forLabel);
  VarType toType = expr();
  if (toType != INT) {
    fail("FOR 'to' expression must be integer");
    return;
  }
  emit("cmp " + varRef + ", EAX");
  emit("jge " + endForLabel);
  while (!token->isKeyword(ENDFOR) && token->type() != END_OF_FILE) {
    statement();
  }
  expect(ENDFOR);
  emit("inc DWORD " + varRef);
  emit("jmp " + forLabel);
  emitLabel(endForLabel);
}

VarType Parser::expr() {
  VarType leftType = atom();
  if (token->type() == SYMBOL) {
    if (leftType == INT) {
      emit("push RAX");
    } else {
      emit("sub RSP, 0x08");
      emit("movq [RSP], XMM0");
    }
    Symbol symbol = token->symbol();
    advance();

    VarType rightType = atom();
    checkTypes(leftType, rightType);
    if (leftType == INT) {
      emit("pop RBX");
    } else {
      // pop XMM1
      emit("movq XMM1, [RSP]");
      emit("add RSP, 0x08");
    }

    string opcode = lookup(arithOpcodes, {symbol, leftType} );
    if (opcode != NOT_FOUND) {
      emit(opcode);
      return leftType;
    }
    // lookup cmp opcodes
    opcode = lookup(cmpOpcodes, {symbol, leftType});
    if (opcode == NOT_FOUND) {
      fail("Cannot find code for opcode " + to_string(symbol));
      return NONE;
    }
    emit(opcode);
    return BOOL_TYPE;
  }
  return leftType;
}

VarType Parser::atom() {
  string value = token->value();
  VarType varType = token->varType();

  switch (token->type()) {
    case CONST:
      switch (varType) {
        case INT:
          emit("mov EAX, " + value);
          advance();
          return varType;
        case STR:
          emit("mov RAX, " + addStringConstant(value));
          advance();
          return varType;
        case FLOAT:
          emit("movq XMM0, [" + addFloatConstant(value) + "]");
          advance();
          return varType;
      }
      break;

    case VAR:
      switch (varType) {
        case INT:
          emit("mov EAX, [_" + value + "]");
          advance();
          return varType;
        case STR:
          emit("mov RAX, [_" + value + "]");
          advance();
          return varType;
        case FLOAT:
          emit("movq XMM0, [_" + value + "]");
          advance();
          return varType;
      }
      break;
  }

  fail("Cannot parse atom " + value);
  return NONE;
}

void Parser::expect(Symbol symbol) {
  if (token->type() != SYMBOL || token->symbol() != symbol) {
    fail("Expected symbol, got " + token->value());
  }
  advance();
}

void Parser::expect(Keyword keyword) {
  if (token->type() != KEYWORD || token->keyword() != keyword) {
    fail("Expected keyword, got " + token->value());
  }
  advance();
}

void Parser::checkTypes(VarType left, VarType right) {
  if (left != right) {
    fail("Type mismatch");
  }
}

string Parser::addStringConstant(string value) {
  string name = lookup(stringTable, value);
  if (name != NOT_FOUND) {
    return name;
  }
  name = nextLabel("CONST");
  stringTable.insert( { value, name });
  string data = name;
  data.append(": db \"").append(value).append("\", 0");
  addData(data);
  return name;
}

string Parser::addFloatConstant(string value) {
  string name = lookup(floatTable, value);
  if (name != NOT_FOUND) {
    return name;
  }
  name = nextLabel("FLOAT");
  floatTable.insert( { value, name });
  string data = name;
  data.append(": dq ").append(value);
  addData(data);
  return name;
}

string Parser::nextLabel(string prefix) {
  return prefix + "_" + to_string(nextInt());
}
