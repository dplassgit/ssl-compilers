#ifndef __lexer_h__
#define __lexer_h__

#include "token.h"
#include <string>

class Lexer {
public:
  Lexer(std::string text);

  Token *nextToken();

private:
  void advance();
  Token *makeString();
  Token *makeNumber();
  Token *makeText();
  Token *makeSymbol();

  std::string text;
  int loc;
  char cc;
};
#endif
