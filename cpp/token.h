#ifndef __token_h__
#define __token_h__

#include "keyword.h"

#include <string>

enum TokenType {
  END_OF_FILE = 0, CONST, VAR, KEYWORD, SYMBOL, NO_TOKEN = -1
};

enum VarType {
  INT = 0, STR, BOOL_TYPE, FLOAT, NONE = -1
};

enum Symbol {
  PLUS = 0, MINUS, MULT, DIV, EQEQ, EQ, NEQ, LEQ, LT, GEQ, GT, NO_SYMBOL = -1
};

static const std::string SYMBOLS[] = {
  "+", "-", "*", "/", "==", "=", "!=", "<=", "<", ">=", ">" };

#define N_SYMBOLS 11

class Token {
public:
  Token::Token(TokenType type) :
      m_type(type), m_value("") {
  }

  Token::Token(TokenType type, std::string value) :
      m_value(value), m_type(type) {
  }

  Token::Token(TokenType type, std::string value, VarType varType) :
      m_value(value), m_type(type), m_varType(varType) {
  }

  std::string value() {
    return m_value;
  }

  TokenType type() {
    return m_type;
  }

  VarType varType() {
    return m_varType;
  }

  Token* keyword(Keyword kw) {
    m_kw = kw;
    return this;
  }

  Keyword keyword() {
    return m_kw;
  }

  Token* symbol(Symbol st) {
    m_symbol = st;
    return this;
  }

  Symbol symbol() {
    return m_symbol;
  }

  bool isKeyword(Keyword kw) {
    return m_type == KEYWORD && m_kw == kw;
  }

private:
  TokenType m_type;
  std::string m_value;
  VarType m_varType = NONE;
  Keyword m_kw = NO_KEYWORD;
  Symbol m_symbol = NO_SYMBOL;
};

#endif
