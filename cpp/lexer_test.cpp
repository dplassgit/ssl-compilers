#include <gtest/gtest.h>
#include <string>

#include "keyword.h"
#include "lexer.h"
#include "token.h"

using namespace std;

TEST(LexerTest, Empty) {
  Lexer *lexer = new Lexer("");
  Token *token = lexer->nextToken();
  EXPECT_EQ(token->type(), END_OF_FILE);
}

TEST(LexerTest, Comment) {
  Lexer *lexer = new Lexer("# comment\n");
  Token *token = lexer->nextToken();
  EXPECT_EQ(token->type(), END_OF_FILE);
}

TEST(LexerTest, IntConstant) {
  Lexer *lexer = new Lexer("123");
  Token *token = lexer->nextToken();
  EXPECT_EQ(token->type(), CONST);
  EXPECT_EQ(token->value(), "123");
  EXPECT_EQ(token->varType(), INT);
}

TEST(LexerTest, StrConstant) {
  Lexer *lexer = new Lexer("\"123\"");
  Token *token = lexer->nextToken();
  EXPECT_EQ(token->type(), CONST);
  EXPECT_EQ(token->value(), "123");
  EXPECT_EQ(token->varType(), STR);
}

TEST(LexerTest, UnclosedString) {
  Lexer *lexer = new Lexer("\"hi");
  EXPECT_EXIT(lexer->nextToken(), testing::ExitedWithCode(-1),
      "Unclosed string literal");
}

TEST(LexerTest, Variables) {
  char *vars[] = {"a", "i", "s", "z"};
  VarType varTypes[] = {FLOAT, INT, STR, STR};
  Lexer *lexer = new Lexer("a i s z");
  for (int i = 0; i < 4; ++i) {
    Token *token = lexer->nextToken();
    EXPECT_EQ(token->type(), VAR);
    EXPECT_EQ(token->value(), vars[i]);
    EXPECT_EQ(token->varType(), varTypes[i]);
  }
}

TEST(LexerTest, UpperVariables) {
  char *vars[] = {"A", "I", "S", "Z"};
  VarType varTypes[] = {FLOAT, INT, STR, STR};
  Lexer *lexer = new Lexer("A I S Z");
  for (int i = 0; i < 4; ++i) {
    Token *token = lexer->nextToken();
    EXPECT_EQ(token->type(), VAR);
    EXPECT_EQ(token->value(), vars[i]);
    EXPECT_EQ(token->varType(), varTypes[i]);
  }
}

static Keyword expectedKeywords[] = {
  FOR, TO, STEP, ENDFOR, IF, THEN, ELSE, ENDIF, PRINT, PRINTLN
};

TEST(LexerTest, EachKeyword) {
  for (int i = 0; i < N_KEYWORDS; ++i) {
    Lexer *lexer = new Lexer(KEYWORDS[i]);
    Token *token = lexer->nextToken();
    EXPECT_EQ(token->type(), KEYWORD);
    EXPECT_EQ(token->keyword(), expectedKeywords[i]);
  }
}

TEST(LexerTest, EachKeywordLower) {
  for (int i = 0; i < N_KEYWORDS; ++i) {
    string keyword = KEYWORDS[i];
    for (auto& c : keyword) {
      c = tolower(c);
    }
    Lexer *lexer = new Lexer(keyword);
    Token *token = lexer->nextToken();
    EXPECT_EQ(token->type(), KEYWORD);
    EXPECT_EQ(token->keyword(), expectedKeywords[i]);
  }
}

TEST(LexerTest, BadKeyword) {
  Lexer *lexer = new Lexer("new");
  EXPECT_EXIT(lexer->nextToken(), testing::ExitedWithCode(-1),
      "Unknown keyword new");
}

static Symbol expectedSymbols[] = {
  PLUS, MINUS, MULT, DIV, EQEQ, EQ, NEQ, LEQ, LT, GEQ, GT
};

TEST(LexerTest, EachSymbol) {
  for (int i = 0; i < N_SYMBOLS; ++i) {
    string symbol = SYMBOLS[i];
    Lexer *lexer = new Lexer(symbol);
    Token *token = lexer->nextToken();
    EXPECT_EQ(token->type(), SYMBOL);
    EXPECT_EQ(token->symbol(), expectedSymbols[i]);
  }
}

TEST(LexerTest, AllSymbols) {
  Lexer *lexer = new Lexer("+-*/===!=<=<>=>");
  for (int i = 0; i < N_SYMBOLS; ++i) {
    Token *token = lexer->nextToken();
    EXPECT_EQ(token->type(), SYMBOL);
    EXPECT_EQ(token->symbol(), expectedSymbols[i]);
  }
}

TEST(LexerTest, InvalidSymbol) {
  Lexer *lexer = new Lexer("%");
  EXPECT_EXIT(lexer->nextToken(), testing::ExitedWithCode(-1),
      "Unknown symbol %");
}

