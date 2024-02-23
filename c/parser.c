#include <stdio.h>
#include <stdlib.h>
#include "lexer.h"
#include "parser.h"
#include "token.h"

static void advance(Parser* this);
static void statements(Parser *this);
static void statement(Parser *this);
static void assignment(Parser *this);
static void parsePrint(Parser *this);
static void parseIf(Parser *this);
static void parseFor(Parser *this);
static VarType expr(Parser *this);
static VarType atom(Parser *this);
static VarType generateFloatArith(SymbolType symbol);
static VarType generateIntArith(SymbolType symbol);
static char *nextLabel(char *prefix);
static void expectKeyword(Parser* this, KeywordType kw);
static void expectSymbol(Parser* this, SymbolType sym);
static void addGlobalData(Parser *this, char *name, VarType type);

Parser *newParser(char* text) {
  Lexer* lexer = newLexer(text);
  Parser* parser = (Parser *)calloc(sizeof(Parser), 1);
  parser->lexer = lexer;
  advance(parser);
  return parser;
}

void emit0(char *line) {
  printf("%s\n", line);
}

void emitf(char *pattern, char *value) {
  printf("  ");
  printf(pattern, value);
  putchar('\n');
}

void emit(char *line) {
  emitf(line, NULL);
}

void emitLabel(char *label) {
  printf("%s:\n", label);
}

void parse(Parser *this) {
  emit0("global main");
  emit0("section .text");
  emit0("main:");
  statements(this);
  emit("extern exit");
  emit("call exit\n");

  if (this->data != NULL) {
    emit0("section .data");
    Data *entry = this->data;
    while (entry != NULL) {
      emit(entry->value);
      entry = entry->next;
    }
  }
}

void statements(Parser *this) {
  while (this->token->tokenType != END_OF_FILE) {
    statement(this);
  }
}

void statement(Parser *this) {
  if (this->token->tokenType == VAR) {
    assignment(this);
    return;
  }
  if (isKeyword(this->token, PRINTLN) || isKeyword(this->token, PRINT)) {
    parsePrint(this);
    return;
  }
  if (isKeyword(this->token, IF)) {
    parseIf(this);
    return;
  }
  if (isKeyword(this->token, FOR)) {
    parseFor(this);
    return;
  }
  fail("Unexpected token %s", this->token->value);
}


void expectKeyword(Parser* this, KeywordType kw) {
  if (this->token->tokenType != KEYWORD) {
    if (this->token->value != NULL) {
      fail("Expected keyword, found %s", this->token->value);
    } else {
      fail("Expected keyword, found %d", this->token->tokenType);
    }
  }
  if (this->token->tokenData.keyword != kw) {
    fail("Expected keyword %s, found %s", KEYWORDS[kw], this->token->value);
  }
  advance(this);
}

void parseIf(Parser *this) {
  advance(this); // eat the "if"
  VarType exprType = expr(this);
  if (exprType != BOOL) {
    fail("Must use BOOL expression in IF");
    return;
  }
  char *elseLabel = nextLabel("else");
  char *endIfLabel = nextLabel("endif");

  emit("cmp AL, 0x01");
  emitf("jne %s", elseLabel);

  expectKeyword(this, THEN);

  while (!isKeyword(this->token, ENDIF) && 
         !isKeyword(this->token, ELSE) &&
         this->token->tokenType != END_OF_FILE) {
    statement(this);
  }
  if (this->token->tokenType == END_OF_FILE) {
    fail("Expected ELSE or ENDIF, found EOF");
  }

  bool hasElse = isKeyword(this->token, ELSE);
  if (hasElse) {
    // only have to jump to end if we are at "else" in the parse
    emitf("jmp %s", endIfLabel);
  }
  emitLabel(elseLabel);

  if (hasElse) {
    advance(this); // eat the else
    while (!isKeyword(this->token, ENDIF) && this->token->tokenType != END_OF_FILE) {
      statement(this);
    }
  }

  expectKeyword(this, ENDIF);

  if (hasElse) {
    emitLabel(endIfLabel);
  }
}

void parseFor(Parser *this) {
  expectKeyword(this, FOR);
  if (this->token->tokenType != VAR) {
    fail("Expected VARIABLE, found  %s", this->token->value);
    return;
  }
  VarType varType = this->token->tokenData.varType;
  if (varType != INT) {
    fail("FOR variable must be integer");
    return;
  }
  char *varName = this->token->value;
  addGlobalData(this, varName, varType);
  advance(this);

  expectSymbol(this, EQ);

  VarType fromType = expr(this);
  if (fromType != INT) {
    fail("FOR 'from' expression must be integer");
    return;
  }
  // Set variable to "from"
  emitf("mov [_%s], EAX", varName);
  expectKeyword(this, TO);

  char *forLabel = nextLabel("for");
  char *endForLabel = nextLabel("endfor");
  emitLabel(forLabel);
  VarType toType = expr(this);
  if (toType != INT) {
    fail("FOR 'to' expression must be integer");
    return;
  }
  emitf("cmp [_%s], EAX", varName);
  emitf("jge %s", endForLabel);
  while (!isKeyword(this->token, ENDFOR) && this->token->tokenType != END_OF_FILE) {
    statement(this);
  }
  expectKeyword(this, ENDFOR);
  emitf("inc DWORD [_%s]", varName);
  emitf("jmp %s", forLabel);
  emitLabel(endForLabel);
}


bool addData(Parser *this, char *value) {
  Data *oldfirst = this->data;
  while (oldfirst != NULL && strcmp(oldfirst->value, value) != 0) {
    oldfirst = oldfirst->next;
  }
  if (oldfirst != NULL && strcmp(oldfirst->value, value) == 0) {
    // Found
    return true;
  }
  Data *newfirst = (Data *) calloc(sizeof(Data), 1);
  newfirst->next = this->data;
  this->data = newfirst;
  newfirst->value = value;
  return false;
}

void addGlobalData(Parser *this, char *name, VarType type) {
  // _x: dd 0
  char *line = (char *)calloc(8 + strlen(name), 1);
  switch(type) {
    case INT:
      sprintf(line, "_%s: dd 0", name);
      break;
    case STR:
    case FLOAT:
      sprintf(line, "_%s: dq 0", name);
      break;
    default:
      fail("Cannot add global data for %s yet", TYPE_NAMES[type]);
      break;
  }
  if (addData(this, line)) {
    // duplicate
    free(line);
  }
}

void expectSymbol(Parser* this, SymbolType sym) {
  if (this->token->tokenType != SYMBOL) {
    fail("Expected symbol, found %s", this->token->value);
  }
  if (this->token->tokenData.symbol != sym) {
    fail("Expected symbol %d, found %s", sym, this->token->value);
  }
  advance(this);
}

void checkTypes(VarType left, VarType right) {
  if (left != right) {
    fail("Incompatible types: %s vs %s", TYPE_NAMES[left], TYPE_NAMES[right]);
  }
}

void assignment(Parser *this) {
  char *name = this->token->value;
  VarType varType = this->token->tokenData.varType;
  addGlobalData(this, name, varType);

  advance(this);

  expectSymbol(this, EQ);

  VarType exprType = expr(this);
  checkTypes(varType, exprType);
  switch (varType) {
    case INT:
      emitf("mov [_%s], EAX", name);
      break;
    case STR:
      emitf("mov [_%s], RAX", name);
      break;
    case FLOAT:
      emitf("movq [_%s], XMM0", name);
      break;
    default:
      fail("Cannot assign to %s yet", TYPE_NAMES[varType]);
  }
}


void parsePrint(Parser* this) {
  bool isPrintln = isKeyword(this->token, PRINTLN);
  advance(this);
  Token *token = this->token;
  VarType exprType = expr(this);
  switch(exprType) {
    case INT:
      addData(this, "INT_FMT: db '%%d', 0");
      emit("mov RCX, INT_FMT");
      emit("mov EDX, EAX");
      break;

    case FLOAT:
      addData(this, "FLOAT_FMT: db '%%.16g', 0");
      emit("mov RCX, FLOAT_FMT");
      emit("movq RDX, XMM0");
      break;

    case STR:
      emit("mov RCX, RAX");
      break;

    case BOOL:
      addData(this, "TRUE: db 'true', 0");
      addData(this, "FALSE: db 'false', 0");
      emit("cmp AL, 1");
      emit("mov RCX, FALSE");
      emit("mov RDX, TRUE");
      emit("cmovz RCX, RDX");
      break;

    default:
      fail("Cannot print type %s", TYPE_NAMES[exprType]);
      return;
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


VarType expr(Parser *this) {
  VarType leftType = atom(this);
  if (this->token->tokenType == SYMBOL) {
    switch(leftType) {
      case INT:
        emit("push RAX");
        break;
      case FLOAT:
        // Push XMM0
        emit("sub RSP, 0x08");
        emit("movq [RSP], XMM0");
        break;
      default:
        fail("Cannot do artihmetic on %s yet", TYPE_NAMES[leftType]);
        break;
    }

    SymbolType symbol = this->token->tokenData.symbol;
    advance(this); // eat the symbol
    VarType rightType = atom(this);
    checkTypes(leftType, rightType);

    switch(leftType) {
      case INT:
        return generateIntArith(symbol);
        break;

      case FLOAT:
        return generateFloatArith(symbol);
        break;
      default:
        fail("Cannot do artihmetic on %s yet", TYPE_NAMES[leftType]);
        break;
    }
  }
  return leftType;

}

void emitCompare(SymbolType symbol) {
  switch(symbol) {
    case EQEQ:
      emit("setz AL");
      break;
    case NEQ:
      emit("setnz AL");
      break;
    case LT:
      emit("setl AL");
      break;
    case LEQ:
      emit("setle AL");
      break;
    case GT:
      emit("setg AL");
      break;
    case GEQ:
      emit("setge AL");
      break;
    default:
      break;
  }
}


VarType generateFloatArith(SymbolType symbol) {
  emit("movq XMM1, [RSP]");
  emit("add RSP, 0x08");
  switch(symbol) {
    case PLUS:
      emit("addsd XMM0, XMM1");
      break;
    case MULT:
      emit("mulsd XMM0, XMM1");
      break;
    case MINUS:
      emit("subsd XMM1, XMM0");
      emit("movq XMM0, XMM1");
      break;
    case DIV:
      emit("divsd XMM1, XMM0");
      emit("movq XMM0, XMM1");
      break;
    case EQEQ:
    case NEQ:
    case LT:
    case LEQ:
    case GT:
    case GEQ:
      emit("comisd XMM1, XMM0");
      emitCompare(symbol);
      return BOOL;

    default:
      fail("Cannot do operation %c yet", symbol);
      break;
  }
  return FLOAT;
}


VarType generateIntArith(SymbolType symbol)  {
  emit("pop RBX");
  switch(symbol) {
    case PLUS:
      emit("add EAX, EBX");
      break;
    case MULT:
      emit("imul EAX, EBX");
      break;
    case MINUS:
      emit("xchg EAX, EBX");
      emit("sub EAX, EBX");
      break;
    case DIV:
      emit("xchg EAX, EBX");
      emit("cdq");
      emit("idiv EBX");
      break;

    case EQEQ:
    case NEQ:
    case LT:
    case LEQ:
    case GT:
    case GEQ:
      emit("cmp EBX, EAX");
      emitCompare(symbol);
      return BOOL;

    default:
      fail("Cannot do operation %c yet", symbol);
      break;
  }
  return INT;
}

static int id = 0;
char *nextLabel(char *prefix) {
  char *label = (char *) malloc(strlen(prefix) + 5);
  sprintf(label, "%s_%d", prefix, id);
  id++;
  return label;
}

Entry *getConstant(Parser *this, VarType varType, char *value) {
  Entry *entry = this->constants;
  while (entry != NULL) {
    if (strcmp(entry->value, value) == 0 && entry->type == varType) {
      // Found
      return entry;
    }
    entry = entry->next;
  }
  return NULL;
}

char *addConstant(Parser *this, VarType varType, char *value) {
  Entry *entry = getConstant(this, varType, value);
  if (entry != NULL) {
    // Found
    return entry->name;
  }
  // Make a new one
  Entry *newfirst = (Entry *) calloc(sizeof(Entry), 1);
  newfirst->next = this->constants;
  char *name = nextLabel(TYPE_NAMES[varType]);
  newfirst->name = name;
  newfirst->value = value;
  newfirst->type = varType;
  this->constants = newfirst;
  char *line = NULL;
  switch(varType) {
    case FLOAT:
      // 30 = 5 plus 25 characters for the number, shrug
      line = (char *)calloc(30 + strlen(name), 1);
      sprintf(line, "%s: dq %s", name, value);
      break;

    case STR:
      line = (char *)calloc(11 + strlen(name) + strlen(value), 1);
      sprintf(line, "%s: db '%s', 0", name, value);
      break;
  }
  if (line != NULL) {
    addData(this, line);
    return name;
  }
  fail("Could not create constant for type %s", TYPE_NAMES[varType]);
  return NULL;
}


VarType atom(Parser *this) {
  Token *t = this->token;
  VarType varType = t->tokenData.varType;
  switch(t->tokenType) {
    case CONST:
      switch(varType) {
        case INT:
          emitf("mov EAX, %s", t->value);
          advance(this);
          return varType;

        case STR: {
          char *name = addConstant(this, varType, t->value);
          emitf("mov RAX, %s", name);
          advance(this);
        }
          return varType;

        case FLOAT: {
          char *name = addConstant(this, varType, t->value);
          emitf("movq XMM0, [%s]", name);
          advance(this);
        }
          return varType;
      }
      break;

    case VAR:
      switch(varType) {
        case INT:
          emitf("mov EAX, [_%s]", t->value);
          advance(this);
          return varType;

        case STR:
          emitf("mov RAX, [_%s]", t->value);
          advance(this);
          return varType;

        case FLOAT:
          emitf("movq XMM0, [_%s]", t->value);
          advance(this);
          return varType;
      }
      break;

    default:
      break;
  }
  fail("Cannot parse atom %s", this->token->value);
  return NO_TYPE;
}

void advance(Parser *this) {
  this->token = nextToken(this->lexer);
}

