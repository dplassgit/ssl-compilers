package main

import "fmt"

type Pair struct {
  k1 SymbolType
  k2 VarType
}

var arithOpcodes = map[Pair]string {
  {Plus, IntVarType}: "add EAX, EBX",
  {Mult, IntVarType}: "imul EAX, EBX",
  {Div, IntVarType}: "xchg EAX, EBX\n  cdq\n  idiv EBX",
  {Minus, IntVarType}: "xchg EAX, EBX\n  sub EAX, EBX",

  {Plus, FloatVarType}: "addsd XMM0, XMM1",
  {Mult, FloatVarType}: "mulsd XMM0, XMM1",
  {Div, FloatVarType}: "divsd XMM1, XMM0\n  movq XMM0, XMM1",
  {Minus, FloatVarType}: "subsd XMM1, XMM0\n  movq XMM0, XMM1",
}

var cmpOpcodes = map[Pair]string {
  {EqEq, IntVarType}: "cmp EBX, EAX\n  setz AL",
  {Neq, IntVarType}: "cmp EBX, EAX\n  setnz AL",
  {Lt, IntVarType}: "cmp EBX, EAX\n  setl AL",
  {Gt, IntVarType}: "cmp EBX, EAX\n  setg AL",
  {Geq, IntVarType}: "cmp EBX, EAX\n  setge AL",
  {Leq, IntVarType}: "cmp EBX, EAX\n  setle AL",

  {EqEq, FloatVarType}: "comisd XMM1, XMM0\n  setz AL",
  {Neq, FloatVarType}: "comisd XMM1, XMM0\n  setnz AL",
  {Lt, FloatVarType}: "comisd XMM1, XMM0\n  setb AL",
  {Gt, FloatVarType}: "comisd XMM1, XMM0\n  seta AL",
  {Leq, FloatVarType}: "comisd XMM1, XMM0\n  setbe AL",
  {Geq, FloatVarType}: "comisd XMM1, XMM0\n  setae AL",
}


type Parser struct {
  lexer Lexer
  token Token
  code []string
  // global data set
  data map[string]bool
  // map from value to name.
  stringTable map[string]string
  floatTable map[string]string
  id int
}

func NewParser(text string) Parser {
  lexer := NewLexer(text)
  parser := Parser{
    lexer: lexer,
    id: 0,
    data: make(map[string]bool),
    stringTable: make(map[string]string),
    floatTable: make(map[string]string),
  }
  parser.advance()
  return parser
}

func (this *Parser) Parse() []string {
  this.emit0("; golang")
  this.emit0("global main")
  this.emit0("section .text")
  this.emit0("main:")
  this.statements()
  this.emit("extern exit")
  this.emit("call exit\n")

  if len(this.data) > 0 {
    this.emit0("section .data")
    for line, _ := range this.data {
      this.emit(line)
    }
  }
  return this.code
}

func (this *Parser) advance() {
  this.token = this.lexer.NextToken()
}

func (this *Parser) statements() {
  for this.token.tokenType != EndOfFile {
    this.statement()
  }
}

func (this *Parser) statement() {
  if this.token.IsKeyword(Print) || this.token.IsKeyword(PrintLn) {
    this.parsePrint()
    return
  }
  if this.token.tokenType == Var {
    this.parseAssignment()
    return
  }
  if this.token.IsKeyword(If) {
    this.parseIf()
    return
  }
  if this.token.IsKeyword(For) {
    this.parseFor()
    return
  }
  panic("Cannot parse statement starting with " + this.token.value)
}

func (this *Parser) parseAssignment() {
  t := this.token
  this.advance()

  this.expect(Eq)

  exprType := this.expr()
  this.checkTypes(t.varType, exprType)

  this.addDataByType(t.value, exprType)
  switch exprType {
    case IntVarType:
      this.emit(fmt.Sprintf("mov [_%s], EAX", t.value))
    case StrVarType:
      this.emit(fmt.Sprintf("mov [_%s], RAX", t.value))
    case FloatVarType:
      this.emit(fmt.Sprintf("movq [_%s], XMM0", t.value))
    default:
      panic("Cannot parse assignment starting with " + t.value)
  }
}

func (this *Parser) parseIf() {
  this.expectKeyword(If)

  condType := this.expr()
  if condType != BoolVarType {
    panic("IF condition must be boolean; was " + varTypeToString[condType])
  }
  elseLabel := this.nextLabel("else")
  endIfLabel := this.nextLabel("endif")

  this.emit("cmp AL, 0x01")
  this.emit("jne " + elseLabel)

  this.expectKeyword(Then)

  for !this.token.IsKeyword(EndIf) && !this.token.IsKeyword(Else) && this.token.tokenType != EndOfFile {
    this.statement()
  }
  if this.token.tokenType == EndOfFile {
    panic("Expected ELSE or ENDIF, found EOF")
  }

  hasElse := this.token.IsKeyword(Else)
  if hasElse {
    // only have to jump to end if we are at "else" in the parse
    this.emit("jmp " + endIfLabel)
  }
  this.emitLabel(elseLabel)

  if hasElse {
    this.advance()  // eat the else
    for !this.token.IsKeyword(EndIf) && this.token.tokenType != EndOfFile {
      this.statement()
    }
  }

  this.expectKeyword(EndIf)

  if hasElse {
    this.emitLabel(endIfLabel)
  }
}

func (this *Parser) parseFor() {
  this.expectKeyword(For)

  if this.token.tokenType != Var {
    panic("Expected VARIABLE, found " + this.token.value)
  }
  varType := this.token.varType
  if varType != IntVarType {
    panic("FOR variable must be INT, was " + varTypeToString[varType])
  }
  varName := this.token.value
  this.addDataByType(varName, varType)
  varRef := "[_" + varName + "]"
  this.advance()

  this.expect(Eq)

  fromType := this.expr()
  if fromType != IntVarType {
    panic("FOR 'from' expression must be INT, was " + varTypeToString[fromType])
  }
  // Set variable to "from"
  this.emit("mov " + varRef + ", EAX")

  this.expectKeyword(To)

  forLabel := this.nextLabel("for")
  this.emitLabel(forLabel)
  endForLabel := this.nextLabel("endfor")

  toType := this.expr()
  if toType != IntVarType {
    panic("FOR 'to' expression must be INT, was " + varTypeToString[toType])
  }

  this.emit("cmp " + varRef + ", EAX")
  this.emit("jge " + endForLabel)
  for !this.token.IsKeyword(EndFor) && this.token.tokenType != EndOfFile {
    this.statement()
  }
  this.expectKeyword(EndFor)

  this.emit("inc DWORD " + varRef)
  this.emit("jmp " + forLabel)
  this.emitLabel(endForLabel)
}

func (this *Parser) parsePrint() {
  isPrintln := this.token.IsKeyword(PrintLn)
  this.advance()
  exprType := this.expr()
  switch exprType {
    case IntVarType:
      this.addData("INT_FMT: db '%d', 0")
      this.emit("mov RCX, INT_FMT")
      this.emit("mov EDX, EAX")

    case BoolVarType:
      this.addData("TRUE: db 'true', 0")
      this.addData("FALSE: db 'false', 0")
      this.emit("cmp AL, 1")
      this.emit("mov RCX, FALSE")
      this.emit("mov RDX, TRUE")
      this.emit("cmovz RCX, RDX")

    case StrVarType:
      this.emit("mov RCX, RAX")

    case FloatVarType:
      this.addData("FLOAT_FMT: db '%.16g', 0")
      this.emit("mov RCX, FLOAT_FMT")
      this.emit("movq RDX, XMM0")

    default:
      panic("Cannot print type " + varTypeToString[exprType])
  }

  this.emit("sub RSP, 0x20")
  this.emit("extern printf")
  this.emit("call printf")
  if isPrintln {
    this.emit("extern putchar")
    this.emit("mov rcx, 10")
    this.emit("call putchar")
  }
  this.emit("add RSP, 0x20")
}

func (this *Parser) expr() VarType {
  leftType := this.atom()
  if this.token.tokenType == Symbol {
    if leftType == StrVarType {
      panic("Cannot combine objects of type " + varTypeToString[leftType])
    }
    if leftType == FloatVarType {
      // Push XMM0
      this.emit("sub RSP, 0x08")
      this.emit("movq [RSP], XMM0")
    } else {
      this.emit("push RAX")
    }
    op := this.token
    this.advance()
    rightType := this.atom()
    this.checkTypes(leftType, rightType)
    if leftType == FloatVarType {
      // pop XMM1
      this.emit("movq XMM1, [RSP]")
      this.emit("add RSP, 0x08")
    } else {
      this.emit("pop RBX")
    }

    code, ok := arithOpcodes[Pair{k1: op.symbol, k2:leftType}]
    if ok {
      this.emit(code)
      return leftType
    }
    code, ok = cmpOpcodes[Pair{k1:op.symbol, k2:leftType}]
    if ok {
      this.emit(code)
      return BoolVarType
    }
    panic("Cannot generate code for op " + op.value)
  }
  return leftType
}

func (this *Parser) atom() VarType {
  switch this.token.tokenType {
    case Const:
    switch this.token.varType {
      case IntVarType:
        this.emit("mov EAX, " + this.token.value)
        this.advance()
        return IntVarType

      case StrVarType:
        name := this.makeStringConstant(this.token.value)
        this.emit("mov RAX, " + name)
        this.advance()
        return StrVarType

      case FloatVarType:
        name := this.makeFloatConstant(this.token.value)
        this.emit(fmt.Sprintf("movq XMM0, [%s]", name))
        this.advance()
        return FloatVarType
    }

    case Var:
    switch this.token.varType {
      case IntVarType:
        this.emit(fmt.Sprintf("mov EAX, [_%s]", this.token.value))
        this.advance()
        return IntVarType

      case StrVarType:
        this.emit(fmt.Sprintf("mov RAX, [_%s]", this.token.value))
        this.advance()
        return StrVarType

      case FloatVarType:
        this.emit(fmt.Sprintf("movq XMM0, [_%s]", this.token.value))
        this.advance()
        return FloatVarType
    }
  }
  panic("Cannot parse atom " + this.token.value)
}

func (this *Parser) makeStringConstant(value string) string {
  name, ok := this.stringTable[value]
  if ok {
    return name
  }
  name = this.nextLabel("CONST")
  this.stringTable[value] = name
  this.addData(fmt.Sprintf("%s: db '%s', 0", name, value))
  return name
}

func (this *Parser) makeFloatConstant(value string) string {
  name, ok := this.floatTable[value]
  if ok {
    return name
  }
  name = this.nextLabel("FLOAT")
  this.floatTable[value] = name
  this.addData(fmt.Sprintf("%s: dq %s", name, value))
  return name
}

func (this *Parser) checkTypes(left VarType, right VarType) {
  if left != right {
    panic(fmt.Sprintf("Type mismatch: %s cannot work with %s",
        varTypeToString[left], varTypeToString[right]))
  }
}

func (this *Parser) expect(expected SymbolType) {
  if this.token.tokenType != Symbol {
    panic(fmt.Sprintf("Expected symbol, was %d", this.token.tokenType))
  }
  if this.token.symbol != expected {
    for s, v := range toSymbolType {
      if v == expected {
        panic(fmt.Sprintf("Expected symbol %s, found %s", s, this.token.value))
      }
    }
    panic(fmt.Sprintf("Expected symbol %d, found %s", expected, this.token.value))
  }
  this.advance()
}

func (this *Parser) expectKeyword(expected KeywordType) {
  if this.token.tokenType != Keyword {
    panic(fmt.Sprintf("Expected keyword, was %d", this.token.tokenType))
  }
  if this.token.keyword != expected {
    for s, v := range toKeywordType {
      if v == expected {
        panic(fmt.Sprintf("Expected keyword %s, found %s", s, this.token.value))
      }
    }
    panic(fmt.Sprintf("Expected symbol %d, found %s", expected, this.token.value))
  }
  this.advance()
}

func (this *Parser) addData(entry string) {
  this.data[entry] = true
}

func (this *Parser) addDataByType(name string, varType VarType) {
  switch varType {
    case IntVarType:
      this.addData("_" + name + ": dd 0")
    case StrVarType:
      this.addData("_" + name + ": dq 0")
    case FloatVarType:
      this.addData("_" + name + ": dq 0.0")
    default:
      panic("Cannot add variable of type " + varTypeToString[varType])
  }
}

func (this *Parser) emit0(line string) {
  this.code = append(this.code, line)
}

func (this *Parser) emitLabel(label string) {
  this.emit0(label + ":")
}

func (this *Parser) emit(line string) {
  this.emit0("  " + line)
}

func (this *Parser) nextLabel(prefix string) string {
  this.id++
  return fmt.Sprintf("%s_%d", prefix, this.id)
}
