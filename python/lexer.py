import fileinput
from enum import Enum

# This is frowned upon in the style guide but...
from ssl_token import *

SYMBOLS = ['+', '-', '*', '/', '=', '==', '!=', '<', '<=', '>', '>=']


class Lexer:

  def __init__(self, text):
    self.text = text
    self.loc = 0
    self.cc = 0
    self.advance()

  def nextToken(self):
    # skip unwanted whitespace
    while True:
      while self.cc == ' ' or self.cc == '\n' or self.cc == '\t' or self.cc == '\r':
        self.advance()
      if self.cc != '#':  # comment
        break
      while self.cc != '\n' and self.cc != '':
        self.advance()

    if self.cc == '':
      return Token(TokenType.EOF, '')

    if self.cc.isdigit():
      return self.makeNumber()
    elif self.cc.isalpha():
      return self.makeText()
    elif self.cc == '"':
      return self.makeString()
    return self.makeSymbol()

  def advance(self):
    if self.loc < len(self.text):
      self.cc = self.text[self.loc]
    else:
      # Indicates no more characters
      self.cc = ''
    self.loc = self.loc + 1
    return self.cc

  def fail(message):
    print(message)
    exit(-1)

  def makeString(self):
    self.advance()
    val = ''
    while self.cc != '' and self.cc != '"':
      val += self.cc
      self.advance()
    if self.cc != '"':
      fail("Unclosed string literal")
    self.advance()
    return Token(TokenType.CONST, val, VarType.STR)

  def makeNumber(self):
    cc = self.cc
    self.advance()
    num = cc
    while self.cc.isdigit():
      num += self.cc
      self.advance()
    if self.cc == '.':
      num += self.cc
      self.advance()
      while self.cc.isdigit():
        num += self.cc
        self.advance()
      return Token(TokenType.CONST, float(num), VarType.FLOAT)

    return Token(TokenType.CONST, int(num), VarType.INT)

  def makeText(self):
    first = self.cc
    self.advance()
    if not self.cc.isalpha():
      # next char is not alphanumeric: it's a variable
      lower_first = first.lower()
      if lower_first >= 'a' and lower_first <= 'h':
        return Token(TokenType.VAR, first, VarType.FLOAT)
      elif lower_first <= 'n':
        return Token(TokenType.VAR, first, VarType.INT)
      else:
        return Token(TokenType.VAR, first, VarType.STR)

    so_far = first
    while self.cc.isalpha():
      so_far += self.cc
      self.advance()
    # look up the keyword
    kw_enum = Keyword[so_far.upper()]
    if not kw_enum:
      fail("Unknown keyword " + so_far)
    return Token(TokenType.KEYWORD, kw_enum)

  def makeSymbol(self):
    first = self.cc
    self.advance()
    maybeTwoCharSymbol = first + self.cc
    if maybeTwoCharSymbol in SYMBOLS:
      # Two-character symbol
      self.advance()
      return Token(TokenType.SYMBOL, maybeTwoCharSymbol)
    if first in SYMBOLS:
      return Token(TokenType.SYMBOL, first)
    fail("Unknown symbol " + first)
