#ifndef __keyword_h__
#define __keyword_h__

#include <string>

enum Keyword {
  FOR = 0,
  TO,
  STEP,
  ENDFOR,
  IF,
  THEN,
  ELSE,
  ENDIF,
  PRINT,
  PRINTLN,
  NO_KEYWORD = -1
};

static const std::string KEYWORDS[] = { "FOR", "TO", "STEP", "ENDFOR", "IF",
    "THEN", "ELSE", "ENDIF", "PRINT", "PRINTLN" };

#endif
