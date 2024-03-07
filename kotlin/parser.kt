package com.plasstech.lang.ssl
import kotlin.system.exitProcess

class Parser(val text: String) {
  private val ARITHMETIC_SYMBOLS = setOf(
      SymbolType.Plus,
      SymbolType.Minus,
      SymbolType.Mult,
      SymbolType.Div)
  private val COMPARISON_SYMBOLS = setOf(
      SymbolType.EqEq,
      SymbolType.Neq,
      SymbolType.Lt,
      SymbolType.Gt,
      SymbolType.Geq,
      SymbolType.Leq)
  private val INT_OPCODES = mapOf(
      SymbolType.Plus to "add EAX, EBX",
      SymbolType.Mult to "imul EAX, EBX",
      SymbolType.Minus to "xchg EAX, EBX\n  sub EAX, EBX",
      SymbolType.Div to "xchg EAX, EBX\n  cdq\n  idiv EBX",
      SymbolType.EqEq to "cmp EBX, EAX\n  setz AL",
      SymbolType.Neq to "cmp EBX, EAX\n  setnz AL",
      SymbolType.Lt to "cmp EBX, EAX\n  setl AL",
      SymbolType.Gt to "cmp EBX, EAX\n  setg AL",
      SymbolType.Leq to "cmp EBX, EAX\n  setle AL",
      SymbolType.Geq to "cmp EBX, EAX\n  setge AL")
  private val FLOAT_OPCODES = mapOf(
      SymbolType.Plus to "addsd XMM0, XMM1",
      SymbolType.Mult to "mulsd XMM0, XMM1",
      SymbolType.Minus to "subsd XMM1, XMM0\n  movq XMM0, XMM1",
      SymbolType.Div to "divsd XMM1, XMM0\n  movq XMM0, XMM1",
      SymbolType.EqEq to "comisd XMM1, XMM0\n  setz AL",
      SymbolType.Neq to "comisd XMM1, XMM0\n  setnz AL",
      SymbolType.Lt to "comisd XMM1, XMM0\n  setb AL",
      SymbolType.Gt to "comisd XMM1, XMM0\n  seta AL",
      SymbolType.Leq to "comisd XMM1, XMM0\n  setbe AL",
      SymbolType.Geq to "comisd XMM1, XMM0\n  setae AL")
  private val OPCODES = mapOf(
      VarType.Int to INT_OPCODES,
      VarType.Float to FLOAT_OPCODES)

  private val lexer = Lexer(text)
  private var token = lexer.nextToken()
  private val data = mutableSetOf<String>()
  // maps value to name
  private val constants = mutableMapOf<Pair<String, VarType>, String>()

  private fun advance() {
    token = lexer.nextToken()
  }

  private fun fail(msg: String) {
    println(msg)
    exitProcess(-1)
  }

  fun parse() {
    emit0("global main")
    emit0("section .text")
    emit0("main:")
    statements()
    emit("extern exit")
    emit("call exit\n")
    if (data.any()) {
      emit0("section .data")
      for (entry in data) {
        emit(entry)
      }
    }
  }

  private fun statements() {
    while (token.type != TokenType.EndOfFile) {
      statement()
    }
  }

  private fun statement() {
    if (token.isKeyword(KeywordType.PRINT) || token.isKeyword(KeywordType.PRINTLN)) {
      parsePrint()
      return
    }
    if (token.type == TokenType.Variable) {
      assignment()
      return
    }
    if (token.isKeyword(KeywordType.IF)) {
      parseIf()
      return
    }
    if (token.isKeyword(KeywordType.FOR)) {
      parseFor()
      return
    }

    fail("Cannot parse token $token")
  }

  private fun parseFor() {
    expectKeyword(KeywordType.FOR)
    if (token.type != TokenType.Variable) {
      fail("Expected VARIABLE, found $token")
    }
    val varType = token.varType()
    if (varType != VarType.Int) {
      fail("FOR variable must be INT, was $varType")
    }
    val varName = token.value
    addData("_$varName: dd 0")
    val varRef = "[_$varName]"
    advance()

    expect(SymbolType.Eq)

    val fromType = expr()
    checkTypes(VarType.Int, fromType)
    // Set variable to "from"
    emit("mov $varRef, EAX")

    expectKeyword(KeywordType.TO)

    val forLabel = nextLabel("for")
    emitLabel(forLabel)
    val endForLabel = nextLabel("endfor")

    val toType = expr()
    checkTypes(VarType.Int, toType)

    emit("cmp $varRef, EAX")
    emit("jge $endForLabel")
    while (!token.isKeyword(KeywordType.ENDFOR) && token.type != TokenType.EndOfFile) {
      statement()
    }
    expectKeyword(KeywordType.ENDFOR)

    emit("inc DWORD $varRef")
    emit("jmp $forLabel")
    emitLabel(endForLabel)
  }

  private fun parseIf() {
    expectKeyword(KeywordType.IF)
    val condType = expr()
    checkTypes(VarType.Bool, condType)

    val elseLabel = nextLabel("else")
    val endIfLabel = nextLabel("endif")

    emit("cmp AL, 0x01")
    emit("jne $elseLabel")

    expectKeyword(KeywordType.THEN)

    while (!token.isKeyword(KeywordType.ENDIF) &&
        !token.isKeyword(KeywordType.ELSE) &&
        token.type != TokenType.EndOfFile) {
      statement()
    }
    if (token.type == TokenType.EndOfFile) {
      fail("Expected ELSE or ENDIF, found EOF")
    }

    val hasElse = token.isKeyword(KeywordType.ELSE)
    if (hasElse) {
      // only have to jump to end if we are at "else" in the parse
      emit("jmp $endIfLabel")
    }
    emitLabel(elseLabel)

    if (hasElse) {
      advance()  // eat the else
      while (!token.isKeyword(KeywordType.ENDIF) && token.type != TokenType.EndOfFile) {
        statement()
      }
    }

    expectKeyword(KeywordType.ENDIF)

    if (hasElse) {
      emitLabel(endIfLabel)
    }
  }

  private fun expect(symbol: SymbolType) {
    if (token.type != TokenType.Symbol || token.symbol() != symbol) {
      fail("Expected $symbol, found $token")
    }
    advance()
  }

  private fun expectKeyword(kw: KeywordType) {
    if (!token.isKeyword(kw)) {
      fail("Expected $kw, found $token")
    }
    advance()
  }

  private fun checkTypes(left: VarType, right: VarType) {
    if (left != right) {
      fail("Type mismatch: expected $left, saw $right")
    }
  }

  private fun assignment() {
    val varName = token.value
    val varType = token.varType()
    advance()
    expect(SymbolType.Eq)
    val exprType = expr()
    checkTypes(varType, exprType)
    when (varType) {
      VarType.Int -> {
        addData("_$varName: dd 0")
        emit("mov [_$varName], EAX")
      }
      VarType.Float -> {
        addData("_$varName: dq 0.0")
        emit("movq [_$varName], XMM0")
      }
      VarType.Str -> {
        addData("_$varName: dq 0")
        emit("mov [_$varName], RAX")
      }
      else -> {
        fail("Cannot assign to $exprType yet")
      }
    }
  }

  private fun parsePrint() {
    val isPrintln = token.isKeyword(KeywordType.PRINTLN)
    advance()  // eat the print or println
    val exprType = expr()
    when (exprType) {
      VarType.Int -> {
        addData("INT_FMT: db '%d', 0")
        emit("mov RCX, INT_FMT")
        emit("mov EDX, EAX")
      }
      VarType.Float -> {
        addData("FLOAT_FMT: db '%.16g', 0")
        emit("mov RCX, FLOAT_FMT")
        emit("movq RDX, XMM0")
      }
      VarType.Str -> emit("mov RCX, RAX")
      VarType.Bool -> {
        addData("TRUE: db 'true', 0")
        addData("FALSE: db 'false', 0")
        emit("cmp AL, 1")
        emit("mov RCX, FALSE")
        emit("mov RDX, TRUE")
        emit("cmovz RCX, RDX")
      }
      else -> fail("Cannot print $exprType yet")
    }
    emit("sub RSP, 0x20")
    emit("extern printf")
    emit("call printf")
    if (isPrintln) {
      emit("extern putchar")
      emit("mov rcx, 10")
      emit("call putchar")
    }
    emit("add RSP, 0x20")
  }

  private fun expr(): VarType {
    val leftType = atom()
    if (token.type == TokenType.Symbol &&
        (ARITHMETIC_SYMBOLS.contains(token.symbol()) ||
         COMPARISON_SYMBOLS.contains(token.symbol()))) {
      when (leftType) {
        VarType.Int -> emit("push RAX")
        VarType.Float -> {
          // push XMM0
          emit("sub RSP, 0x08");
          emit("movq [RSP], XMM0");
        }
        else -> fail("Cannot generate arithmetic for $leftType")
      }
      val op = token.symbol()
      advance()
      val rightType = atom()
      checkTypes(leftType, rightType)
      when (leftType) {
        VarType.Int -> emit("pop RBX")
        VarType.Float -> {
          // pop XMM0
          emit("movq XMM1, [RSP]");
          emit("add RSP, 0x08");
        }
        else -> fail("Cannot generate arithmetic for $leftType")
      }
      return generateArithmetic(leftType, op)
    }
    return leftType
  }

  private fun generateArithmetic(varType: VarType, op: SymbolType): VarType {
    val opcodeMap = OPCODES[varType]
    if (opcodeMap == null) {
      fail("Cannot generate code for $varType")
      return VarType.Bool
    }
    val retValType = when(ARITHMETIC_SYMBOLS.contains(op)) {
      true -> varType
      false -> VarType.Bool
    }
    val opcodes = opcodeMap[op]
    if (opcodes == null) {
      fail("Cannot generate code for $op")
      return VarType.NoType
    }
    emit(opcodes)
    return retValType
  }

  private fun generateIntArithmetic(op: SymbolType): VarType {
    val opcodes = INT_OPCODES[op]
    if (opcodes == null) {
      // TODO: maybe generate compare
      fail("Cannot generate code for $op")
      return VarType.Bool
    }
    emit(opcodes)
    return VarType.Int
  }

  private fun atom(): VarType {
    when (token.type) {
      TokenType.Constant -> {
        when (token.varType()) {
          VarType.Int -> {
            emit("mov EAX, ${token.value}")
            advance()
            return VarType.Int
          }
          VarType.Float -> {
            val name = addFloatConstant(token.value)
            emit("movq XMM0, [$name]")
            advance()
            return VarType.Float
          }
          VarType.Str -> {
            val name = addStringConstant(token.value)
            emit("mov RAX, $name")
            advance()
            return VarType.Str
          }
          else -> fail("Cannot parse atom $token yet")
        }
      }
      TokenType.Variable -> {
        val name = token.value
        when (token.varType()) {
          VarType.Int -> {
            emit("mov EAX, [_$name]")
            advance()
            return VarType.Int
          }
          VarType.Float -> {
            emit("movq XMM0, [_$name]")
            advance()
            return VarType.Float
          }
          VarType.Str -> {
            emit("mov RAX, [_$name]")
            advance()
            return VarType.Str
          }
          else -> fail("Cannot parse atom $token yet")
        }
      }
      else -> fail("Cannot parse atom $token yet")
    }

    fail("Cannot parse atom $token yet")
    return VarType.NoType
  }

  private fun emit0(line: String) {
    println(line)
  }

  private fun emitLabel(label: String) {
    println("$label:")
  }

  private fun emit(line: String) {
    println("  $line")
  }

  private fun addData(entry: String) {
    data.add(entry)
  }

  private var id = 0
  private fun nextLabel(prefix: String): String {
    return "${prefix}_${id++}"
  }

  private fun addConstant(value: String, varType: VarType): String {
    val key = Pair(value, varType)
    val existingName = constants[key]
    if (existingName != null) {
      return existingName
    }
    val name = nextLabel(varType.toString().uppercase())
    constants[key] = name
    return name
  }

  private fun addFloatConstant(value: String): String {
    val name = addConstant(value, VarType.Float)
    addData("$name: dq $value")
    return name
  }

  private fun addStringConstant(value: String): String {
    val name = addConstant(value, VarType.Str)
    addData("$name: db '$value', 0")
    return name
  }

}
