from enum import Enum

# Token types
class TokenType(Enum):
  CONST = 1
  VAR = 2
  KEYWORD = 3
  SYMBOL = 4
  EOF = 999

SYMBOLS=['+', '-', '*', '/', '%', '<', '>', '=', '[', ']']
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
      return Token(TokenType.CONST, float(num), VarType.FLOAT)

    return Token(TokenType.CONST, int(num), VarType.INT)

  def makeText(self):
    cc = self.cc
    self.advance()
    if not self.cc.isalpha():
      # next char is not alphanumeric: it's a variable
      if cc >= 'a' and cc <= 'h':
        return Token(TokenType.VAR, cc, VarType.FLOAT)
      elif cc >= 'i' and cc <= 'n':
        return Token(TokenType.VAR, cc, VarType.INT)
      else:
        return Token(TokenType.VAR, cc, VarType.STR)
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


OPCODES = {
  ('+', VarType.INT): "add eax, ebx",
  ('*', VarType.INT): "imul eax, ebx",
  ('/', VarType.INT): "xchg eax, ebx\n  cdq\n  idiv ebx", # eax=eax/ebx
  ('-', VarType.INT): "xchg eax, ebx\n  sub eax, ebx"
}

CMP_OPCODES = {
  ('==', VarType.INT): "setz",
  ('!=', VarType.INT): "setnz",
  ('<', VarType.INT): "setl",
  ('>', VarType.INT): "setg",
  ('<=', VarType.INT): "setle",
  ('>=', VarType.INT): "setge"
}

class Parser:

  def __init__(self, text):
    self.lexer = Lexer(text)
    self.token = None
    self.data = set()

  def advance(self):
    self.token = self.lexer.nextToken()
    print("  ; %s" % str(self.token))
    return self.token

  def fail(self, msg=None):
    if msg:
      print("%s at token %s" % (msg, str(self.token)))
    else:
      print("Unexpected token %s" % str(self.token))
    exit(-1)

  def parse(self):
    print("global main")
    print("section .text")
    print("main:")
    self.advance()
    # Read statements until eof
    while not self.token.isEof():
      self.statement()
    print("  extern exit")
    print("  call exit\n")
    if len(self.data):
      print("section .data")
      for entry in self.data:
        print("  %s" % entry)

  def statement(self):
    if self.token.tokenType == TokenType.VAR:
      self.assignment()
      return
    if self.token.tokenType != TokenType.KEYWORD:
      self.fail()
      return
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
    varType = self.token.varType
    if varType == VarType.INT:
      self.addData("_%s: dd 0" % var)
    else:
      self.fail()
      return
    self.advance()
    if self.token.value != '=':
      self.fail()
      return
    self.advance()
    exprType = self.expr()
    if varType != exprType:
      self.fail("Cannot assign %s to %s" % (exprType, varType))
    if varType == VarType.INT:
      print("  mov [_%s], EAX" % var)
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
        self.addData("INT_NL_FMT: db '%d', 10, 0")
        print("  mov RCX, INT_NL_FMT")
      else:
        self.addData("INT_FMT: db '%d', 0")
        print("  mov RCX, INT_FMT")
      print("  mov EDX, EAX")
      print("  sub RSP, 0x20")
      print("  extern printf")
      print("  call printf")
      print("  add RSP, 0x20")
      return
    elif exprType == VarType.BOOL:
      self.addData("TRUE: db 'true', 0")
      self.addData("FALSE: db 'false', 0")
      print("  cmp al, 1")
      print("  mov RCX, FALSE")
      print("  mov RDX, TRUE")
      print("  cmovz RCX, RDX")
      print("  sub RSP, 0x20")
      print("  extern printf")
      print("  call printf")
      if is_println:
        print("  extern putchar")
        print("  mov rcx, 10")
        print("  call putchar")
      print("  add RSP, 0x20")
      return

    self.fail("Cannot print of type %s" % exprType)

  def parseFor(self):
    self.fail("Cannot generate FOR yet")

  def expr(self):
    leftType = self.atom()
    if self.token.tokenType == TokenType.SYMBOL:
      print("  push rax")
      op = self.token.value
      self.advance()
      rightType = self.atom()
      if leftType != rightType:
        self.fail("Cannot apply %s to %s" % (leftType, rightType))
      print("  pop rbx")  # rbx was old left
      opcode = OPCODES.get((op, leftType))
      if opcode:
        print("  %s" % opcode)
        return leftType
      opcode = CMP_OPCODES.get((op, leftType))
      if not opcode:
        self.fail("Unknown opcode for op %s" % op)
      # Not sure why not eax, ebx, but that's what v0.d does
      print("  cmp ebx, eax")
      print("  %s al" % opcode)
      return VarType.BOOL
    return leftType

  def atom(self):
    if self.token.tokenType == TokenType.CONST:
      if self.token.varType == VarType.INT:
        const = self.token.value
        print("  mov EAX, %s" % const)
        self.advance()
        return VarType.INT
    elif self.token.tokenType == TokenType.VAR:
      if self.token.varType == VarType.INT:
        self.addData("_%s: dd 0" % self.token.value)
        print ("  mov EAX, [_%s]" % self.token.value)
        self.advance()
        return VarType.INT
    self.fail("atom")


def main():
  text = '''
  i = 0
  # comment
  if f == 345.678 then # more comments
    println i + 1234
  endif
  '''
  one = "i=3 print i println 345"
  add = "i=3 j=4 k=i+j println k"
  mult = "i=3 j=4 k=i*j println k"
  div = "i=12345 j=34 k=i/j println k"
  sub = "i=12345 j=34 k=j-1 println k"
  cmp = "i=12345 j=34 println i!=j"
  program = cmp
  p = Parser(program)
  p.parse()


if __name__ == '__main__':
  main()
