BEGIN {
  FS = "" # input field separator

  program = ""

  # Location within "program"
  loc = 0
  # Current character.
  cc = -1

  # Index into "token" array
  TokenType = 0
  Value = 1
  VarType = 2

  token[TokenType] = ""
  token[Value] = ""
  token[VarType] = ""

  # Token types
  TokenTypeEof = "eof"
  TokenTypeConst = "const"
  TokenTypeKeyword = "keyword"
  TokenTypeSymbol = "symbol"
  TokenTypeVar = "var"

  # VarTypes
  VarTypeFloat = "float"
  VarTypeInt = "int"
  VarTypeString = "string"
  VarTypeBool = "bool"

  # Keywords
  keywords["print"] = 1
  keywords["println"] = 1
  keywords["if"] = 1
  keywords["then"] = 1
  keywords["else"] = 1
  keywords["endif"] = 1
  keywords["for"] = 1
  keywords["to"] = 1
  keywords["step"] = 1
  keywords["endfor"] = 1

  # Symbols, duh.
  symbols["+"] = 1
  symbols["-"] = 1
  symbols["*"] = 1
  symbols["/"] = 1
  symbols["="] = 1
  symbols["=="] = 1
  symbols["!="] = 1
  symbols["<"] = 1
  symbols["<="] = 1
  symbols[">"] = 1
  symbols[">="] = 1

  # Keys are symbols + vartype
  arithCode["+" VarTypeInt] = "add EAX, EBX"
  arithCode["*" VarTypeInt] = "imul EAX, EBX"
  arithCode["-" VarTypeInt] = "xchg EAX, EBX\n  sub EAX, EBX"
  arithCode["/" VarTypeInt] = "xchg EAX, EBX\n  cdq\n  idiv EBX"
  arithCode["+" VarTypeFloat] = "addsd XMM0, XMM1"
  arithCode["*" VarTypeFloat] = "mulsd XMM0, XMM1"
  arithCode["-" VarTypeFloat] = "subsd XMM1, XMM0\n  movsd XMM0, XMM1"
  arithCode["/" VarTypeFloat] = "divsd XMM1, XMM0\n  movsd XMM0, XMM1"

  # For comparisons
  cmpOpcode[VarTypeInt] = "cmp EBX, EAX"
  cmpOpcode[VarTypeFloat] = "comisd XMM1, XMM0"

  setxOpcode["==" VarTypeInt] = "setz"
  setxOpcode["!=" VarTypeInt] = "setnz"
  setxOpcode["<" VarTypeInt] = "setl"
  setxOpcode[">" VarTypeInt] = "setg"
  setxOpcode["<=" VarTypeInt] = "setle"
  setxOpcode[">=" VarTypeInt] = "setge"
  setxOpcode["==" VarTypeFloat] = "setz"
  setxOpcode["!=" VarTypeFloat] = "setnz"
  setxOpcode["<" VarTypeFloat] = "setb"
  setxOpcode[">" VarTypeFloat] = "seta"
  setxOpcode["<=" VarTypeFloat] = "setbe"
  setxOpcode[">=" VarTypeFloat] = "setae"

  dataEntries[""] = ""
  stringValues[""] = ""
  floatValues[""] = ""

  id = 0
}


#################################################################
##
## LEXER
##
#################################################################

function skipWhitespace() {
  while (1) {
    while (loc < length(program) && (cc == " " || cc == "\n")) {
      advance()
    }
    if (loc == length(program)) {
      return
    }
    if (cc == "#") {
      advance()
      while (loc < length(program) && cc != "\n") {
        advance()
      }
    }
    return
  }
}

function advance() {
  loc = loc + 1
  cc = -1
  if (loc < length(program)) {
    cc = substr(program, loc, 1)
  }
}

function isAlpha(c) {
  return match(c, /^[a-zA-Z]/)
}

function isNumber(c) {
  return match(c, /[0-9]/)
}

# Token is an array:
#   first element is the token type
#   second element is the value as a string
#   third element is the vartype
function nextToken(token) {
  token[TokenType] = TokenTypeEof
  token[Value] = ""
  token[VarType] = ""
  if (loc == length(program)) {
    return
  }
  skipWhitespace()

  if (isAlpha(cc)) {
    makeTextToken(token)
    return
  }
  if (isNumber(cc)) {
    makeNumber(token)
    return
  }
  if (cc == "\"") {
    makeStringConstant(token)
    return
  }
  makeSymbol(token)
}

function makeStringConstant(token) {
  advance() # eat the open quote
  soFar = ""
  while (cc != -1 && cc != "\"") {
    soFar = soFar cc
    advance()
  }
  if (cc == -1) {
    fail("unclosed string constant")
  }
  advance() # eat the closing quote
  token[TokenType] = TokenTypeConst
  token[Value] = soFar
  token[VarType] = VarTypeString
}

function makeSymbol(token) {
  first = cc
  advance()
  maybeTwo = first cc
  token[TokenType] = TokenTypeSymbol
  if (maybeTwo in symbols) {
    token[Value] = maybeTwo
    advance() # eat the 2nd symbol
  } else if (first in symbols) {
    token[Value] = first
    # don't "advance" here because we already advanced past the first
  } else {
    fail("unknown symbol " first)
  }
}

function makeNumber(token) {
  soFar = cc
  advance()
  while (cc != -1 && isNumber(cc)) {
    soFar = soFar cc
    advance()
  }
  token[TokenType] = TokenTypeConst
  if (cc != ".") {
    # Int constant
    token[Value] = soFar
    token[VarType] = VarTypeInt
    return
  }

  # Float constant
  soFar = soFar cc
  advance() # eat the dot
  while (cc != -1 && isNumber(cc)) {
    soFar = soFar cc
    advance()
  }
  token[Value] = soFar
  token[VarType] = VarTypeFloat
}

function makeTextToken(token) {
  first = cc
  advance()
  if (!isAlpha(cc)) {
    first = tolower(first)
    # got a variable
    token[TokenType] = TokenTypeVar
    token[Value] = first

    # figure out the right type
    if (first < "i") {
      token[VarType] = VarTypeFloat
    } else if (first <= "n") {
      token[VarType] = VarTypeInt
    } else {
      token[VarType] = VarTypeString
    }

  } else {
    # Keyword
    token[TokenType] = TokenTypeKeyword
    soFar = first
    while (cc != -1 && isAlpha(cc)) {
      soFar = soFar cc
      advance()
    }
    soFar = tolower(soFar)
    if (soFar in keywords) {
      token[Value] = soFar
    } else {
      fail("Unknown keyword " soFar)
    }
  }
}


#################################################################
##
## PARSER
##
#################################################################

function parse() {
  print "global main"
  print "section .text"
  print "main:"

  nextToken(token)
  statements(token)

  print "  extern exit"
  print "  call exit\n"

  if (length(dataEntries)) {
    print "section .data"
    for (entry in dataEntries) {
      if (length(entry)) {
        print "  " entry
      }
    }
  }
}

function fail(message) {
  print "Error: " message
  exit -1
}

function addData(entry) {
  # It's basically a "set".
  dataEntries[entry] = entry
}

function addConstEntry(value, varType) {
  if (varType == VarTypeString) {
    name = stringValues[value]
    if (name != "") {
      return name
    }
    name = nextLabel("CONST_")
    stringValues[value] = name
    addData(name ": db '" value "', 0")
    return name
  } else if (varType == VarTypeFloat) {
    name = floatValues[value]
    if (name != "") {
      return name
    }
    name = nextLabel("FLOAT_")
    floatValues[value] = name
    addData(name ": dq " value)
    return name
  }
}


function statements(token) {
  # Read statements until eof
  while (token[TokenType] != TokenTypeEof) {
    statement(token)
  }
}

function statement(token) {
  if (token[TokenType] == TokenTypeKeyword) {
    if (token[Value] == "print" || token[Value] == "println") {
      parsePrint(token)
      return
    }
    if (token[Value] == "if") {
      parseIf(token)
      return
    }
    if (token[Value] == "for") {
      parseFor(token)
      return
    }
  }
  if (token[TokenType] == TokenTypeVar) {
    parseAssignment(token)
    return
  }

  fail("Unknown token type " token[TokenType] " value " token[Value])
}

function nextLabel(prefix) {
  id++
  return prefix "_" id
}

function parseFor(token) {
  expectKeyword(token, "for")
  if (token[TokenType] != TokenTypeVar) {
    fail("Expected VAR for for variable")
  }
  checkTypes(VarTypeInt, token[VarType])

  var = token[Value]
  addData("_" var ": dd 0")
  nextToken(token)

  expectSymbol(token, "=")
  startType = expr(token)
  checkTypes(VarTypeInt, startType)
  print "  mov [_" var "], EAX"

  expectKeyword(token, "to")

  startForLabel = nextLabel("startFor")
  endForLabel = nextLabel("endFor")

  print startForLabel ":"
  endType = expr(token)
  checkTypes(VarTypeInt, endType)
  print "  cmp [_" var "], EAX"
  print "  jge " endForLabel

  while (token[TokenType] != TokenTypeEof && !isKeyword(token, "endfor")) {
    statement(token)
  }
  expectKeyword(token, "endfor")

  print "  inc DWORD [_" var "]"
  print "  jmp " startForLabel
  print endForLabel ":"
}


function parseIf(token) {
  expectKeyword(token, "if")
  exprType = expr(token)
  checkTypes(VarTypeBool, exprType)
  expectKeyword(token, "then")

  elseLabel = nextLabel("else")
  endIfLabel = nextLabel("endIfLabel")
  # if false, go to else
  print "  cmp AL, 0"
  print "  jz " elseLabel

  # now emit statements until ELSE or ENDIF
  while (token[TokenType] != TokenTypeEof && !isKeyword(token, "else") && !isKeyword(token, "endif")) {
    statement(token)
  }
  if (token[TokenType] == TokenTypeEof) {
    fail("Unclosed if/endif")
  }
  hasElse = token[Value] == "else"
  if (hasElse) {
    # We just finished the "then" part, didn't go to "else",
    # so we're done
    print "  jmp " endIfLabel
  }
  print elseLabel ":"
  if (hasElse) {
    nextToken(token) # eat the "else"
    while (token[TokenType] != TokenTypeEof && !isKeyword(token, "endif")) {
      statement(token)
    }
  }
  expectKeyword(token, "endif")
  if (hasElse) {
    print endIfLabel ":"
  }
}

function isKeyword(token, kw) {
  return token[TokenType] == TokenTypeKeyword && token[Value] == kw
}

function parseAssignment(token) {
  varName = token[Value]
  varType = token[VarType]
  nextToken(token)

  expectSymbol(token, "=")

  exprType = expr(token)
  checkTypes(varType, exprType)

  if (varType == VarTypeInt) {
    addData("_" varName ": dd 0")
    print("  mov [_" varName "], EAX")
  } else if (varType == VarTypeString) {
    addData("_" varName ": dq 0")
    print("  mov [_" varName "], RAX")
  } else if (varType == VarTypeFloat) {
    addData("_" varName ": dq 0")
    print("  movq [_" varName "], XMM0")
  } else {
    fail("Cannot assign to " varType " yet")
  }
}

function checkTypes(leftType, rightType, message) {
  if (leftType != rightType) {
    fail("Type mismatch: left " leftType " not compatible with " rightType)
  }
}

function expectSymbol(token, symbol) {
  if (token[TokenType] == TokenTypeSymbol && token[Value] == symbol) {
    nextToken(token)
  } else {
    fail("Expected " symbol ", saw " token[Value])
  }
}

function expectKeyword(token, kw) {
  if (token[TokenType] == TokenTypeKeyword && token[Value] == kw) {
    nextToken(token)
  } else {
    fail("Expected " kw ", saw " token[Value])
  }
}

function parsePrint(token) {
  is_println = token[Value] == "println"
  nextToken(token)
  exprType = expr(token)

  if (exprType == VarTypeInt) {
    addData("INT_FMT: db '%d', 0")
    print "  mov RCX, INT_FMT"
    print "  mov EDX, EAX"
  } else if (exprType == VarTypeString) {
    print "  mov RCX, RAX"
  } else if (exprType == VarTypeBool) {
    addData("TRUE: db 'true', 0")
    addData("FALSE: db 'false', 0")
    print "  cmp AL, 1"
    print "  mov RCX, FALSE"
    print "  mov RDX, TRUE"
    print "  cmovz RCX, RDX"
  } else if (exprType == VarTypeFloat) {
    addData("FLOAT_FMT: db '%.16g', 0")
    print "  mov RCX, FLOAT_FMT"
    print "  movq RDX, XMM0"
  } else {
    fail("Cannot print " exprType " yet")
  }
  print "  sub RSP, 0x20"
  print "  extern printf"
  print "  call printf"
  if (is_println) {
    print "  extern putchar"
    print "  mov RCX, 10"
    print "  call putchar"
  }

  print "  add RSP, 0x20"
}

function expr(token) {
  leftType = atom(token)
  if (token[TokenType] == TokenTypeSymbol) {
    if (leftType == VarTypeFloat) {
      # Push XMM0
      print("  sub RSP, 0x08")
      print("  movq [RSP], XMM0")
    } else {
      print "  push RAX"
    }
    sym = token[Value]
    nextToken(token)
    rightType = atom(token)
    checkTypes(leftType, rightType)
    if (leftType == VarTypeFloat) {
      # pop XMM1
      print "  movq XMM1, [RSP]"
      print "  add RSP, 0x08"
    } else {
      print "  pop RBX"
    }
    if (leftType == VarTypeInt || leftType == VarTypeFloat) {
      return arithmetic(leftType, sym)
    } else {
      fail("Cannot do " sym " yet")
    }
  }
  return leftType
}

function arithmetic(varType, sym) {
  key = sym varType
  if (key in arithCode) {
    print "  " arithCode[key]
    return varType
  } else if (key in setxOpcode) {
    print "  " cmpOpcode[varType]
    print "  " setxOpcode[key] " AL"
    return VarTypeBool
  }
  fail("Cannot do " sym " yet")
}

function atom(token) {
  varType = token[VarType]
  if (token[TokenType] == TokenTypeConst) {
    if (token[VarType] == VarTypeInt) {
      print "  mov EAX, " token[Value]
      nextToken(token)
      return varType
    } else if (token[VarType] == VarTypeString) {
      name = addConstEntry(token[Value], varType)
      print "  mov RAX, " name
      nextToken(token)
      return varType
    } else if (token[VarType] == VarTypeFloat) {
      name = addConstEntry(token[Value], varType)
      print "  movq XMM0, [" name "]"
      nextToken(token)
      return varType
    }
  } else if (token[TokenType] == TokenTypeVar) {
    if (token[VarType] == VarTypeInt) {
      print "  mov EAX, [_" token[Value] "]"
      nextToken(token)
      return varType
    } else if (token[VarType] == VarTypeString) {
      print "  mov RAX, [_" token[Value] "]"
      nextToken(token)
      return varType
    } else if (token[VarType] == VarTypeFloat) {
      print "  movq XMM0, [_" token[Value] "]"
      nextToken(token)
      return varType
    }
  }
  fail("atom Cannot process token type:" token[TokenType] " value:" token[Value] " vartype:" token[VarType])
}


# Make one big string, newline separated
{program = program $0 "\n"}

END {
  # now process it
  advance()
  parse()
}

