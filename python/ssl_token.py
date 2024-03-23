import fileinput
from enum import Enum


# Token types
class TokenType(Enum):
  CONST = 1
  VAR = 2
  KEYWORD = 3
  SYMBOL = 4
  EOF = 999


# Keyword types
class Keyword(Enum):
  IF = 1
  THEN = 2
  ELSE = 3
  ENDIF = 4
  FOR = 5
  TO = 6
  STEP = 7
  ENDFOR = 8
  PRINT = 9
  PRINTLN = 10


class VarType(Enum):
  INT = 1
  FLOAT = 2
  STR = 3
  BOOL = 4
  NONE = 0


class Token:
  def __init__(self, tokenType, value, varType=VarType.NONE):
    self.tokenType = tokenType
    self.value = value
    self.varType = varType

  def __str__(self):
    return "%s, %s" % (self.tokenType, self.value)

  def isEof(self):
    return self.tokenType == TokenType.EOF

  def isKeyword(self, kw):
    return self.tokenType == TokenType.KEYWORD and self.value == kw
