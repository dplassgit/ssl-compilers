#ifndef __parser_h__
#define __parser_h__

#include "lexer.h"
#include "token.h"

typedef struct Data {
  char *value;
  struct Data *next;
} Data;

typedef struct Entry {
  char *value;
  char *name;
  VarType type;
  struct Entry *next;
} Entry;

typedef struct Parser {
  Lexer *lexer;
  Token *token;
  Data *data;
  Entry *constants;
} Parser;


Parser *newParser(char *text);
void parse(Parser *this);

#endif
