#include <stdio.h>
#include <stdlib.h>
#include "lexer.h"
#include "sslc.h"
#include "token.h"


static Token *makeNumber(Lexer *this);
static Token *makeSymbol(Lexer *this);
static Token *makeText(Lexer *this);
static Token *makeString(Lexer *this);
static void advance(Lexer *this);

char *KEYWORDS[] = {
  "IF",
  "THEN",
  "ELSE",
  "ENDIF",
  "FOR",
  "TO",
  "STEP",
  "ENDFOR",
  "PRINT",
  "PRINTLN",
};


Lexer *newLexer(char *text) {
  Lexer *this = (Lexer *)calloc(sizeof(Lexer), 1);
  this->text = text;
  this->loc = text;
  advance(this);
  return this;
}


void advance(Lexer *this) {
  char *loc = this->loc;
  if (*loc != 0) {
    this->cc = *loc;
  } else {
    this->cc = 0;
  }
  this->loc++;
}


Token *nextToken(Lexer *this) {
  while (1) {
    while (this->cc <= ' ' && this->cc != 0) { // anything below 32 is whitespace.
      advance(this);
    }
    if (this->cc != '#') {
      break;
    }
    while (this->cc != '\n' && this->cc != 0) {
      advance(this);
    }
  }
  if (this->cc == 0) {
    return newToken(END_OF_FILE, NULL);
  }
  if (isdigit(this->cc)) {
    return makeNumber(this);
  }
  if (isalpha(this->cc)) {
    return makeText(this);
  }
  if (this->cc == '"') {
    return makeString(this);
  }
  return makeSymbol(this);
}


Token *makeString(Lexer *this) {
  char *start = this->loc;
  advance(this); // eat the starting quote
  while (this->cc != '"' && this->cc != 0) {
    advance(this);
  }
  if (this->cc == 0) {
    fail("Unclosed string literal");
  }
  advance(this); // eat the closing quote
  char *end = this->loc;
  int len = end - start - 1;
  char *literal = (char*)calloc(len, 0);
  strncpy(literal, start, len - 1);
  Token *t = newToken(CONST, literal);
  t->tokenData.varType = STR;
  return t;
}


Token *makeNumber(Lexer *this) {
  char *start = this->loc;
  while (isdigit(this->cc)) {
    advance(this);
  }
  VarType varType = INT;
  if (this->cc == '.') {
    advance(this);
    while (isdigit(this->cc)) {
      advance(this);
    }
    varType = FLOAT;
  }

  // now, *loc points at a non-digit
  int length = this->loc - start;
  char *value = (char*) calloc(length, 1);
  // subtract one because we started one past "first"
  strncpy(value, start - 1, length);
  Token *token = newToken(CONST, value);
  token->tokenData.varType = varType;
  return token;
}

Token *makeText(Lexer *this) {
  char *start = this->loc;
  char first = this->cc;
  advance(this);
  if (!isalpha(this->cc)) {
    // one letter 
    VarType varType = STR;
    if (first >= 'a' && first <= 'h') {
      varType = FLOAT;
    }
    if (first >= 'i' && first <= 'n') {
      varType = INT;
    }
    char *value = (char*) calloc(2, 1);
    value[0] = first;
    Token *token = newToken(VAR, value);
    token->tokenData.varType = varType;
    return token;
  }
  while (isalpha(this->cc)) {
    advance(this);
  }

  // this->loc points to the first non-alpha
  int length = this->loc - start;
  char *value = (char*) calloc(length, 1);
  // subtract one because we started one past "first"
  strncpy(value, start - 1, length);
  for (int i = 0; i < N_KEYWORDS; ++i) {
    if (stricmp(KEYWORDS[i], value) == 0) {
      Token *token = newToken(KEYWORD, value);
      token->tokenData.keyword = i;
      return token;
    }
  }
  fail("ERROR: Unknown keyword: %s", value);
  return NULL;
}

Token *makeSymbol(Lexer *this) {
  char first = this->cc;
  advance(this);
  if (first == '!') {
    if (this->cc == '=') {
      advance(this);
      Token *t = newToken(SYMBOL, "!=");
      t->tokenData.symbol = NEQ;
      return t;
    }
    fail("Unknown symbol !");
    return NULL;
  }

  SymbolType st = -1;
  char *value = NULL;
  // second char
  switch(first) {
    case '*':
      st = MULT;
      break;
    case '+':
      st = PLUS;
      break;
    case '-':
      st = MINUS;
      break;
    case '/':
      st = DIV;
      break;
    default:
      if (this->cc == '=') {
        switch (first) {
          case '=':
            st = EQEQ;
            value = "==";
            advance(this);
            break;
          case '>':
            st = GEQ;
            value =">=";
            advance(this);
            break;
          case '<':
            st = LEQ;
            value = "<=";
            advance(this);
            break;

          default:
            fail("Unknown symbol %c (%d) before =", first, first);
            break;
        }
      } else {
        switch (first) {
          case '=':
            st = EQ;
            break;
          case '>':
            st = GT;
            break;
          case '<':
            st = LT;
            break;
          default:
            fail("Unknown symbol %c (%d) not before =", first, first);
            break;
        }
      }
      break;
  }

  Token *t = newToken(SYMBOL, value);
  t->tokenData.symbol = st;
  return t;
}
