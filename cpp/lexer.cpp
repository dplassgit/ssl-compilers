#include <algorithm>
#include <iostream>
#include <string>

#include "keyword.h"
#include "lexer.h"
#include "token.h"

using namespace std;

Lexer::Lexer(string text) :
    text(text), loc(0), cc(0) {
  this->advance();
}

Token* Lexer::nextToken() {
  while (1) {
    while (cc == ' ' || cc == '\n' || cc == '\t' || cc == '\r') {
      advance();
    }
    if (cc != '#') {
      break;
    }
    while (cc != '\n' && cc != 0) {
      advance();
    }
  }
  if (cc == 0) {
    return new Token(END_OF_FILE);
  }
  if (isdigit(cc)) {
    return makeNumber();
  }
  if (isalpha(cc)) {
    return makeText();
  }
  if (cc == '"') {
    return makeString();
  }
  return makeSymbol();
}

void Lexer::advance() {
  cc = 0;
  if (loc < text.length()) {
    cc = text[loc];
    loc++;
  }
}

Token* Lexer::makeNumber() {
  char first = cc;
  string value(1, first);
  advance();
  while (isdigit(cc)) {
    value += cc;
    advance();
  }
  if (cc != '.') {
    return new Token(CONST, value, INT);
  }
  value += cc;
  advance();
  while (isdigit(cc)) {
    value += cc;
    advance();
  }
  return new Token(CONST, value, FLOAT);
}

string str_toupper(string s) {
  transform(s.begin(), s.end(), s.begin(), [](char c) {
    return toupper(c);
  });
  return s;
}

Token* Lexer::makeText() {
  char first = cc;
  string value(1, first);
  advance();
  if (!isalpha(cc)) {
    // next char is not alpha: it's a variable
    char lowerFirst = tolower(first);
    VarType varType = STR;
    if (lowerFirst >= 'a' && lowerFirst <= 'h') {
      varType = FLOAT;
    } else if (lowerFirst <= 'n') {
      varType = INT;
    }
    return new Token(VAR, value, varType);
  }

  while (isalpha(cc) && cc != 0) {
    value += cc;
    advance();
  }
  Token *t = new Token(KEYWORD, value);
  string uppervalue = str_toupper(value);
  for (int i = 0; i < N_KEYWORDS; ++i) {
    if (uppervalue == KEYWORDS[i]) {
      return t->keyword((Keyword) i);
    }
  }
  cerr << "Unknown keyword " << value << endl;
  exit(-1);
  return NULL;
}

Token* Lexer::makeString() {
  advance();
  string literal;
  while (cc != '"' && cc != 0) {
    literal += cc;
    advance();
  }
  if (cc == 0) {
    cerr << "Unclosed string literal" << endl;
    exit(-1);
    return NULL;
  }
  advance(); // eat the ending quote
  return new Token(CONST, literal, STR);
}

Token* Lexer::makeSymbol() {
  char first = cc;
  string value(1, first);
  advance();

  // See if it's a 2-char symbol
  string maybeTwoChar(1, first);
  maybeTwoChar += cc;
  for (int i = 0; i < N_SYMBOLS; ++i) {
    if (maybeTwoChar == SYMBOLS[i]) {
      advance();  // eat the 2nd char
      Symbol st = (Symbol) i;
      Token *t = new Token(SYMBOL, maybeTwoChar);
      return t->symbol(st);
    }
  }
  for (int i = 0; i < N_SYMBOLS; ++i) {
    if (value == SYMBOLS[i]) {
      Symbol st = (Symbol) i;
      Token *t = new Token(SYMBOL, value);
      return t->symbol(st);
    }
  }
  cerr << "Unknown symbol " << value << endl;
  exit(-1);
  return NULL;
}
