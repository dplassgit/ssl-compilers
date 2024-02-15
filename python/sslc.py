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

class VarType(Enum):
  INT = 1
  FLOAT = 2
  STR = 3
  BOOL = 4

TOKEN_TYPE_TO_VARTYPE = {
    TokenType.INT_CONST: VarType.INT,
    TokenType.INT_VAR: VarType.INT,
    TokenType.FLOAT_CONST: VarType.FLOAT,
    TokenType.FLOAT_VAR: VarType.FLOAT,
    TokenType.STR_CONST: VarType.STR,
    TokenType.STR_VAR: VarType.STR
   }


class Token:
  def __init__(self, tokenType, value):
    self.tokenType = tokenType
    self.value = value

  def __str__(self):
    return str([self.tokenType, self.value])

  def isEof(self):
    return self.tokenType == TokenType.EOF

  def varType(self):
    return TOKEN_TYPE_TO_VARTYPE[self.tokenType]

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
      return Token(TokenType.EOF, '')

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
      return Token(TokenType.FLOAT_CONST, float(num))

    return Token(TokenType.INT_CONST, int(num))

  def makeText(self):
    cc = self.cc
    self.advance()
    if not self.cc.isalpha():
      # next char is not alphanumeric: it's a variable
      if cc >= 'a' and cc <= 'h':
        return Token(TokenType.FLOAT_VAR, cc)
      elif cc >= 'i' and cc <= 'n':
        return Token(TokenType.INT_VAR, cc)
      else:
        return Token(TokenType.STR_VAR, cc)
    kw = cc
    while self.cc.isalpha():
      kw += self.cc
      self.advance()
    # look up the keyword
    kw_enum = KEYWORDS.get(kw)
    if not kw_enum:
      print("Unknown keyword " + kw)
      exit(-1)
    return Token(TokenType.KEYWORD, kw_enum)

  def makeSymbol(self):
    cc = self.cc
    self.advance()
    if cc == '!':
      if self.cc == '=':
        self.advance()
        return Token(TokenType.SYMBOL, '!=')
      print("Unknown symbol " + cc + self.cc)
      exit(-1)

    if cc in EQ_FOLLOWS and self.cc == '=':
      self.advance()
      return Token(TokenType.SYMBOL, cc + "=")
    if cc in SYMBOLS:
      return Token(TokenType.SYMBOL, cc)
    print("Unknown symbol " + cc)
    exit(-1)


class Parser:

  def __init__(self, text):
    self.lexer = Lexer(text)
    self.token = None
    self.data = set()

  def advance(self):
    self.token = self.lexer.nextToken()
    print("  ; %s" % str(self.token))
    return self.token

  def fail(self):
    print("Unknown token %s" % str(self.token))
    exit(-1)

  def parse(self):
    self.advance()
    # Read statements until eof
    print("global main")
    print("section .text")
    print("main:")
    while not self.token.isEof():
      self.statement()
    print("  extern exit")
    print("  call exit\n")
    if len(self.data):
      print("section .data")
      for entry in self.data:
        print("  %s" % entry)

  def statement(self):
    if self.token.tokenType in (TokenType.INT_VAR, TokenType.FLOAT_VAR, TokenType.STR_VAR):
      self.assignment()
      return
    if self.token.tokenType != TokenType.KEYWORD:
      print("Unknown token " + self.token[1])
      exit()
    if self.token.value == Keyword.IF:
      self.parseIf()
      return
    if self.token.value in (Keyword.PRINT, Keyword.PRINTLN):
      self.parsePrint()
      return
    if self.token.value == Keyword.FOR:
      self.parseFor()
      return
    self.fail()

  def assignment(self):
    var = self.token.value
    self.addData("_%s: dq 0" % var)
    varType = self.token.varType()
    self.advance()
    if self.token.value != '=':
      print(";  bad?")
      self.fail()
      return
    self.advance()
    exprType = self.expr()
    # TODO: check exprtype
    if varType != exprType:
      print("Cannot assign %s to %s", exprType, varType)
      exit(-1)
    if varType == VarType.INT:
      print("  mov [_%s], RAX" % var)
      return
    self.fail()

  def parseIf(self):
    self.fail()
    pass

  def addData(self, entry):
    self.data.add(entry)

  def parsePrint(self):
    is_println = self.token.value == Keyword.PRINTLN
    self.advance()
    exprType = self.expr()
    if exprType == VarType.INT:
      if is_println:
        self.addData("INT_NL_FMT: db '%d', 10")
        print("  mov RCX, INT_NL_FMT")
      else:
        self.addData("INT_FMT: db '%d', 0")
        print("  mov RCX, INT_FMT")
      print("  mov RDX, RAX")
      print("  sub RSP, 0x20")
      print("  extern printf")
      print("  call printf")
      print("  add RSP, 0x20")
      return
    self.fail()

  def parseFor(self):
    self.fail()

  def expr(self):
    return self.atom()

  def atom(self):
    if self.token.tokenType == TokenType.INT_CONST:
      const = self.token.value
      print("  mov RAX, %s" % const)
      self.advance()
      return VarType.INT
    elif self.token.tokenType == TokenType.INT_VAR:
      self.addData("_%s: dq 0" % self.token.value)
      print ("  mov RAX, [_%s]" % self.token.value)
      self.advance()
      return VarType.INT
    elif self.token.tokenType == TokenType.SYMBOL and self.token.value == '(':
      self.advance()
      varType = self.expr()
      if self.token.tokenType == TokenType.SYMBOL and self.token.value == ')':
        self.advance()
        return varType
    self.fail()


def main():
  text = '''
  i = 0
  # comment
  if f == 345.678 then # more comments
    println i + 1234
  endif
  '''
  p = Parser("i=(123) println (234)")
  p.parse()


if __name__ == '__main__':
  main()
