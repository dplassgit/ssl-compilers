package com.plasstech.lang.ssl

enum class TokenType {
  Keyword,
  Symbol,
  Variable,
  Constant,
  EndOfFile,
}

enum class KeywordType {
  // These must be uppercase so we can match them.
  IF,
  THEN,
  ELSE,
  ENDIF,
  FOR,
  TO,
  STEP,
  ENDFOR,
  PRINTLN,
  PRINT,
}

enum class SymbolType(val value: String) {
  Plus("+"),
  Minus("-"),
  Mult("*"),
  Div("/"),
  EqEq("=="),
  Eq("="),
  Neq("!="),
  Lt("<"),
  Gt(">"),
  Leq("<="),
  Geq(">="),
  NoSymbol("")
}

enum class VarType {
  Int,
  Str,
  Float,
  Bool,
  NoType,
}

open class Token(val type: TokenType, val value: String) {
  open fun keyword(): KeywordType {
    throw IllegalArgumentException("Non-keyword Token")
  }

  open fun varType(): VarType {
    throw IllegalArgumentException("Non-typed Token")
  }

  open fun symbol(): SymbolType {
    throw IllegalArgumentException("Non-symbol Token")
  }

  override fun toString(): String {
    return "Token type: $type, value: $value"
  }

  fun isKeyword(kw: KeywordType): Boolean {
    return type == TokenType.Keyword && kw == keyword()
  }
}

class KeywordToken : Token {
  private val keyword: KeywordType

  constructor(value: String, keyword: KeywordType) :
          super(TokenType.Keyword, value) {
    this.keyword = keyword
  }

  override fun keyword(): KeywordType {
    return keyword
  }

  override fun toString(): String {
    return super.toString() + ", keyword: $keyword"
  }
}

class TypedToken : Token {
  private val varType: VarType

  constructor(type: TokenType, value: String, varType: VarType) :
          super(type, value) {
    this.varType = varType
  }

  override fun varType(): VarType {
    return varType
  }

  override fun toString(): String {
    return super.toString() + ", varType: $varType"
  }
}

class SymbolToken : Token {
  private val symbolType: SymbolType

  constructor(value: String, symbolType: SymbolType) :
          super(TokenType.Symbol, value) {
    this.symbolType = symbolType
  }

  override fun symbol(): SymbolType {
    return symbolType
  }

  override fun toString(): String {
    return super.toString() + ", symbolType: $symbolType"
  }
}
