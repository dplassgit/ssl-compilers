#ifndef __token_h__
#define __token_h__

#include <stdbool.h>

typedef enum {
  INT,
  FLOAT,
  STR,
  BOOL,
  NO_TYPE = -1
} VarType;

static char *TYPE_NAMES[] = {
  "INT",
  "FLOAT",
  "STR",
  "BOOL"
};

typedef enum {
  IF,
  THEN,
  ELSE,
  ENDIF,
  FOR,
  TO,
  STEP,
  ENDFOR,
  PRINT,
  PRINTLN
} KeywordType;

typedef enum {
  MULT='*',
  PLUS='+',
  MINUS='-',
  DIV='/',
  LT='<',
  EQ='=',
  GT='>',
  EQEQ=91,
  NEQ,
  LEQ,
  GEQ
} SymbolType;

typedef enum {
  END_OF_FILE,
  VAR,
  CONST,
  KEYWORD,
  SYMBOL
} TokenType;


typedef struct Token {
  TokenType tokenType;
  char *value;

  union {
    KeywordType keyword;
    SymbolType symbol;
    VarType varType;
  } tokenData;
} Token;

Token *newToken(TokenType type, char *value);
void printToken(Token *this);
bool isKeyword(Token *this, KeywordType kw);

#endif
