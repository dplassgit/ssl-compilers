#include <stdbool.h>
#include <stdlib.h>
#include "token.h"

Token *newToken(TokenType type, char *value) {
  Token *token = (Token *)calloc(sizeof(Token), 1);
  token->value = value;
  token->tokenType = type;
  return token;
}

void printToken(Token *this) {
  printf("\nToken type %d\n", this->tokenType);
  if (this->tokenType == VAR || this->tokenType == CONST) {
    printf("Token val |%s|\n", this->value);
    printf("Token vartype %s\n", TYPE_NAMES[this->tokenData.varType]);
  } else if (this->tokenType == KEYWORD) {
    printf("Token val |%s|\n", this->value);
    printf("Token keyword %d\n", this->tokenData.keyword);
  } else if (this->tokenType == SYMBOL) {
    if (this->tokenData.symbol < EQEQ) {
      printf("Token val |%c|\n", this->tokenData.symbol);
    } else {
      printf("Token val |%s|\n", this->value);
      printf("Token symbol %d\n", this->tokenData.symbol);
    }
  }
}

bool isKeyword(Token *this, KeywordType kw) {
  if (this->tokenType != KEYWORD) {
    return false;
  }
  return this->tokenData.keyword == kw;
}
