from enum import Enum

# Token types
class TokenType(Enum):
  INT_CONST = 1
  INT_VAR = 2
  FLOAT_CONST = 3
  FLOAT_VAR = 4
  STR_CONST = 5
  STR_VAR = 6
  KEYWORD = 7
  SYMBOL = 8
  EOF = 999

SYMBOLS=['+', '-', '*', '/', '%', '<', '>', '=', '(', ')', '[', ']']
EQ_FOLLOWS=['<', '>', '=']

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
KEYWORDS = {
    'if': Keyword.IF,
    'then':Keyword.THEN,
    'else':Keyword.ELSE,
    'endif':Keyword.ENDIF,
    'for':Keyword.FOR,
    'to':Keyword.TO,
    'step':Keyword.STEP,
    'endfor':Keyword.ENDFOR,
    'print':Keyword.PRINT,
    'println':Keyword.PRINTLN
}


class Lexer:

  def __init__(self, text):
    self.text = text
    self.loc = 0
    self.cc = 0
    self.advance()

  def advance(self):
    if self.loc < len(self.text):
      self.cc = self.text[self.loc]
    else:
      # Indicates no more characters
      self.cc = ''
    self.loc = self.loc + 1
    return self.cc

  # Token is a tuple: (type, value)
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
      return (TokenType.EOF, '')
      
    if self.cc.isdigit():
      return self.makeNumber()
    elif self.cc.isalpha():
      return self.makeText()
    # TODO: deal with string constants
    return self.makeSymbol()

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
      return (TokenType.FLOAT_CONST, float(num))
    
    return (TokenType.INT_CONST, int(num))

  def makeText(self):
    cc = self.cc
    self.advance()
    if not self.cc.isalpha():
      # next char is not alphanumeric: it's a variable
      if cc >= 'a' and cc <= 'h':
        return (TokenType.FLOAT_VAR, cc)
      elif self.cc >= 'i' and self.cc <= 'n':
        return (TokenType.INT_VAR, cc)
      else:
        return (TokenType.STR_VAR, cc)
    kw = cc
    while self.cc.isalpha():
      kw += self.cc
      self.advance()
    # look up the keyword
    kw_enum = KEYWORDS.get(kw)
    if not kw_enum:
      print("Unknown keyword " + kw)
      exit(-1)
    return (TokenType.KEYWORD, kw_enum)

  def makeSymbol(self):
    cc = self.cc
    self.advance()
    if cc == '!':
      if self.cc == '=':
        self.advance()
        return (TokenType.SYMBOL, '!=')
      print("Unknown symbol " + cc + self.cc)
      exit(-1)
      
    if cc in EQ_FOLLOWS and self.cc == '=':
      self.advance()
      return (TokenType.SYMBOL, cc + "=")
    if cc in SYMBOLS:
      return (TokenType.SYMBOL, cc)
    print("Unknown symbol " + cc)
    exit(-1)


class Parser:

  def __init__(self, text):
    self.lexer = Lexer(text)
    self.advance()

  def advance(self):
    self.token = self.lexer.nextToken()
    print(self.token[1])
    return self.token

  def parse(self):
    pass


def main():
  text = '''
  i = 0
  # comment
  if f == 345.678 then # more comments
    println i + 1234
  endif
  '''
  lex = Lexer(text)
  token = lex.nextToken()
  while token[0] != TokenType.EOF:
    print(token[0], token[1])
    token = lex.nextToken()
  # p = Parser(text)
  # p.parse()


if __name__ == '__main__':
  main()
