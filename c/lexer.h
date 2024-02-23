#ifndef __lexer_h__
#define __lexer_h__

#include "token.h"

typedef struct Lexer {
  char *text;
  char *loc;
  char cc;   // current char. equivalent to *loc
} Lexer;


Lexer *newLexer(char *text);
Token *nextToken(Lexer *this);
char *KEYWORDS[];
#define N_KEYWORDS 10

#endif
