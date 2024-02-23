#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>

#include "lexer.h"
#include "parser.h"
#include "token.h"
#include "sslc.h"

char *readProgram() {
  char *program=(char*)calloc(1024,1);
  read(0, program, 1023);
  return program;
}
void parseStdIn() {
  //char *program = "i=123 k=i i=456 println k";
  char *program = readProgram();
  Parser *parser = newParser(program);
  parse(parser);
}

void tokens() {
  //char *program = "* / + - = == != < <= >= >"; // 123 45.6 a i if THEN endFOR";
  char *program = readProgram();
  printf("program:\n%s\n", program);
  Lexer *lexer = newLexer(program);
  Token *token = nextToken(lexer);
  while (token->tokenType != END_OF_FILE) {
    printToken(token);
    token = nextToken(lexer);
  }
  printToken(token);
}

void fail(char *fmt, ...) {
  va_list arg;
  va_start(arg, fmt);
  vprintf(fmt, arg);
  va_end(arg);
  exit(-1);
}

void main(int argc, char **args) {
  if (argc > 1) {
    char *flag = args[1];
    if (strcmp(flag, "-tokensonly") == 0) {
      tokens();
      exit(0);
    }
  }

  parseStdIn();
}

