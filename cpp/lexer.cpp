#include "lexer.h"
#include "keyword.h"
#include "token.h"
#include <algorithm>
#include <iostream>
#include <string>

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
  if (loc < text.length()) {
    cc = text[loc];
    loc++;
  } else {
    cc = 0;
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
  if (cc == '.') {
    value += cc;
    advance();
    while (isdigit(cc)) {
      value += cc;
      advance();
    }
    return new Token(CONST, value, FLOAT);
  }
  return new Token(CONST, value, INT);
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
    VarType varType = STR;
    if (first >= 'a' && first <= 'h') {
      varType = FLOAT;
    }
    if (first >= 'i' && first <= 'n') {
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
  for (int i = 0; i < 10; ++i) {
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
    cerr << "Expected closing double quote, found EOF" << endl;
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
  if (first == '!') {
    if (cc == '=') {
      value += cc;
      advance();
      Token *t = new Token(SYMBOL, value);
      return t->symbol(NEQ);
    }
    cerr << "Unknown symbol !" << endl;
    exit(-1);
    return NULL;
  }

  Symbol st = NO_SYMBOL;
  if (cc == '=') {
    switch (first) {
    case '=':
      st = EQEQ;
      value += cc;
      advance();
      break;
    case '>':
      st = GEQ;
      value += cc;
      advance();
      break;
    case '<':
      st = LEQ;
      value += cc;
      advance();
      break;
    default:
      break;
    }
  }
  if (st == NO_SYMBOL) {
    for (int i = 0; i < 11; ++i) {
      if (value == SYMBOLS[i]) {
        st = (Symbol) i;
        break;
      }
    }
  }

  if (st == NO_SYMBOL) {
    cerr << "Unknown symbol " << value << endl;
    exit(-1);
    return NULL;
  }
  Token *t = new Token(SYMBOL, value);
  return t->symbol(st);
}
