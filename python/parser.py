import fileinput
from enum import Enum

# This is frowned upon in the style guide but...
from ssl_token import *
from lexer import Lexer

OPCODES = {
  ('+', VarType.INT): "add EAX, EBX",
  ('*', VarType.INT): "imul EAX, EBX",
  ('/', VarType.INT): "xchg EAX, EBX\n  cdq\n  idiv EBX",  # EAX=EAX/EBX
  ('-', VarType.INT): "xchg EAX, EBX\n  sub EAX, EBX",

  ('+', VarType.FLOAT): "addsd XMM0, XMM1",
  ('-', VarType.FLOAT): "subsd XMM1, XMM0\n  movsd XMM0, XMM1",
  ('*', VarType.FLOAT): "mulsd XMM0, XMM1",
  ('/', VarType.FLOAT): "divsd XMM1, XMM0\n  movsd XMM0, XMM1",
}

CMP_OPCODE = {
  VarType.INT: "cmp EBX, EAX",
  VarType.FLOAT: "comisd XMM1, XMM0",
}

SETx_OPCODE = {
  ('==', VarType.INT): "setz",
  ('!=', VarType.INT): "setnz",
  ('<', VarType.INT): "setl",
  ('>', VarType.INT): "setg",
  ('<=', VarType.INT): "setle",
  ('>=', VarType.INT): "setge",

  ('==', VarType.FLOAT): "setz",
  ('!=', VarType.FLOAT): "setnz",
  ('<', VarType.FLOAT): "setb",
  ('>', VarType.FLOAT): "seta",
  ('<=', VarType.FLOAT): "setbe",
  ('>=', VarType.FLOAT): "setae"
}


class Parser:

  def __init__(self, text):
    self.lexer = Lexer(text)
    self.token = None
    self.data = set()
    self.labelindex = 0
    self.stringTable = {}
    self.floatTable = {}

  def advance(self):
    self.token = self.lexer.nextToken()
    # print("  ; %s" % str(self.token))
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
    self.statements()
    print("  extern exit")
    print("  call exit\n")
    if len(self.data):
      print("section .data")
      for entry in self.data:
        print("  %s" % entry)

  def statements(self, kw_terminals=[]):

    def stopit(tok):
      if not len(kw_terminals):
        return False
      return tok.tokenType == TokenType.KEYWORD and tok.value in kw_terminals

    while not self.token.isEof() and not stopit(self.token):
      self.statement()

  def statement(self):
    if self.token.tokenType == TokenType.VAR:
      self.assignment()
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
    elif varType == VarType.STR:
      self.addData("_%s: dq 0" % var)
    elif varType == VarType.FLOAT:
      self.addData("_%s: dq 0.0" % var)
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
    elif varType == VarType.STR:
      print("  mov [_%s], RAX" % var)
      return
    elif varType == VarType.FLOAT:
      print("  movq [_%s], XMM0" % var)
      return
    self.fail()

  def nextLabel(self):
    self.labelindex += 1
    return self.labelindex

  def expect(self, kw):
    if self.token.isKeyword(kw):
      self.advance()
      return
    self.fail("Expected %s" % kw)

  def parseIf(self):
    self.advance()
    exprType = self.expr()
    if exprType != VarType.BOOL:
      self.fail("IF must use bool expression")
    self.expect(Keyword.THEN)

    elseindex = self.nextLabel()
    endifindex = self.nextLabel()
    # if false, go to else
    print("  cmp AL, 0")
    print("  jz else_%d" % elseindex)

    # now emit statements until ELSE or ENDIF
    self.statements([Keyword.ELSE, Keyword.ENDIF])
    hasElse = self.token.isKeyword(Keyword.ELSE)
    if hasElse:
      # Go to the end
      print("  jmp endif_%d" % endifindex)

    print("else_%d:" % elseindex)
    if hasElse:
      self.advance()
      self.statements([Keyword.ENDIF])
    self.expect(Keyword.ENDIF)
    if hasElse:
      print("endif_%d:" % endifindex)

  def addData(self, entry):
    self.data.add(entry)

  def parsePrint(self):
    is_println = self.token.value == Keyword.PRINTLN
    self.advance()
    exprType = self.expr()

    def println():
      if is_println:
        print("  extern putchar")
        print("  mov RCX, 10")
        print("  call putchar")

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
      print("  cmp AL, 1")
      print("  mov RCX, FALSE")
      print("  mov RDX, TRUE")
      print("  cmovz RCX, RDX")
      print("  sub RSP, 0x20")
      print("  extern printf")
      print("  call printf")
      println()
      print("  add RSP, 0x20")
      return
    elif exprType == VarType.STR:
      print("  mov RCX, RAX")
      print("  sub RSP, 0x20")
      print("  extern printf")
      print("  call printf")
      println()
      print("  add RSP, 0x20")
      return
    elif exprType == VarType.FLOAT:
      self.addData("FLOAT_FMT: db '%.16g', 0")
      print("  mov RCX, FLOAT_FMT")
      print("  movq RDX, XMM0")
      print("  sub RSP, 0x20")
      print("  extern printf")
      print("  call printf")
      println()
      print("  add RSP, 0x20")
      return

    self.fail("Cannot print of type %s" % exprType)

  def parseFor(self):
    self.expect(Keyword.FOR)

    if self.token.tokenType != TokenType.VAR:
      self.fail("Expected VAR")
    if self.token.varType != VarType.INT:
      self.fail("FOR variable must be integer")

    var = self.token.value
    self.addData("_%s: dd 0" % var)
    self.advance()

    if self.token.tokenType != TokenType.SYMBOL or self.token.value != "=":
      self.fail("Expected =")
    self.advance()
    startType = self.expr()
    if startType != VarType.INT:
      self.fail("FOR start condition must be integer")
    print("  mov [_%s], EAX" % var)

    self.expect(Keyword.TO)

    startfor = self.nextLabel()
    endfor = self.nextLabel()

    print("startfor_%d:" % startfor)
    endType = self.expr()
    if endType != VarType.INT:
      self.fail("FOR end condition must be integer")
    print("  cmp [_%s], EAX" % var)
    print("  jge endfor_%d" % endfor)

    self.statements([Keyword.ENDFOR])
    print("  inc DWORD [_%s]" % var)
    print("  jmp startfor_%d" % startfor)
    print("endfor_%d:" % endfor)
    self.expect(Keyword.ENDFOR)

  def expr(self):
    leftType = self.atom()
    if self.token.tokenType == TokenType.SYMBOL:
      if leftType == VarType.FLOAT:
        # Push XMM0
        print("  sub RSP, 0x08")
        print("  movq [RSP], XMM0")
      else:
        print("  push RAX")
      op = self.token.value
      self.advance()
      rightType = self.atom()
      if leftType != rightType:
        self.fail("Cannot apply %s to %s" % (leftType, rightType))
      if leftType == VarType.FLOAT:
        # pop XMM1
        print("  movq XMM1, [RSP]")
        print("  add RSP, 0x08")
      else:
        print("  pop rbx")  # rbx was old left
      opcode = OPCODES.get((op, leftType))
      if opcode:
        print("  %s" % opcode)
        return leftType
      opcode = CMP_OPCODE.get(leftType)
      if not opcode:
        self.fail("Unknown opcode for op %s" % op)
      # Not sure why not EAX, EBX, but that's what v0.d does
      print("  %s" % opcode)
      setx = SETx_OPCODE.get((op, leftType))
      # print("  cmp EBX, EAX")
      print("  %s AL" % setx)
      return VarType.BOOL
    return leftType

  def addStringConstant(self, const):
    name = self.stringTable.get(const)
    if name:
      return name
    # make a name
    name = "CONST_%d" % self.nextLabel()
    self.stringTable[const] = name
    self.addData('%s: db "%s", 0' % (name, const))
    return name

  def addFloatConstant(self, const):
    name = self.floatTable.get(const)
    if name:
      return name
    # make a name
    name = "FLOAT_%d" % self.nextLabel()
    self.floatTable[const] = name
    self.addData('%s: dq %s' % (name, const))
    return name

  def atom(self):
    varType = self.token.varType
    if self.token.tokenType == TokenType.CONST:
      if varType == VarType.INT:
        const = self.token.value
        print("  mov EAX, %s" % const)
        self.advance()
        return varType
      elif varType == VarType.STR:
        const = self.token.value
        name = self.addStringConstant(const)
        print("  mov RAX, %s" % name)
        self.advance()
        return varType
      elif varType == VarType.FLOAT:
        const = self.token.value
        name = self.addFloatConstant(const)
        print("  movq XMM0, [%s]" % name)
        self.advance()
        return varType
    elif self.token.tokenType == TokenType.VAR:
      if varType == VarType.INT:
        print("  mov EAX, [_%s]" % self.token.value)
        self.advance()
        return varType
      elif varType == VarType.STR:
        print("  mov RAX, [_%s]" % self.token.value)
        self.advance()
        return varType
      elif varType == VarType.FLOAT:
        print("  movq XMM0, [_%s]" % self.token.value)
        self.advance()
        return varType
    self.fail("Cannot parse atom")
