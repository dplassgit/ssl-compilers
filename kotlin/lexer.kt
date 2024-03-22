package com.plasstech.lang.ssl

class Lexer(val text: String) {
  private val ZERO = '\u0000'

  private var loc = 0 // current location in the text
  private var cc = ZERO // current character

  // This must be after declaring "cc" and "loc" because otherwise
  // they get reset!
  init {
    advance()
  }

  private fun advance() {
    if (loc < text.length) {
      cc = text[loc]
    } else {
      // Indicates no more characters
      cc = ZERO
    }
    loc++
  }

  fun nextToken(): Token {
    // skip unwanted whitespace
    while (true) {
      while (cc == ' ' || cc == '\n' || cc == '\t' || cc == '\r') {
        advance()
      }
      if (cc != '#') { // # comment
        break
      }
      while (cc != '\n' && cc != '\r' && cc != ZERO) {
        advance()
      }
    }

    if (cc == ZERO) {
      return Token(TokenType.EndOfFile, "")
    }

    if (Character.isDigit(cc)) {
      return makeNumber()
    } else if (Character.isLetter(cc)) {
      return makeText()
    } else if (cc == '"') {
      return makeString()
    }
    return makeSymbol()
  }

  private fun makeSymbol(): Token {
    val first = cc.toString()
    advance()

    if (cc != ZERO) {
      val maybeTwoCharSymbol = first + cc.toString()
      for (symbol in SymbolType.values()) {
        if (symbol.value == maybeTwoCharSymbol) {
          advance() // eat the 2nd symbol
          return SymbolToken(maybeTwoCharSymbol, symbol)
        }
      }
    }
    for (symbol in SymbolType.values()) {
      if (symbol.value == first) {
        // don't eat the first symbol; it was already eaten.
        return SymbolToken(first, symbol)
      }
    }
    throw IllegalStateException("Unknown symbol $first")
  }


  private fun makeString(): Token {
    advance() // eat the opening "
    var literal = ""
    while (cc != '"' && cc != ZERO) {
      literal += cc.toString()
      advance()
    }
    if (cc == ZERO) {
      throw IllegalStateException("Unclosed string literal")
    }
    advance() // eat the closing "
    return TypedToken(TokenType.Constant, literal, VarType.Str)
  }


  private fun makeText(): Token {
    val first = cc
    advance()
    if (!Character.isLetter(cc)) {
      val firstLower = first.lowercase()[0]
      // one letter variable
      if (firstLower < 'i') {
        // a-h
        return TypedToken(TokenType.Variable, first.toString(), VarType.Float)
      }
      if (firstLower <= 'n') {
        // i-n
        return TypedToken(TokenType.Variable, first.toString(), VarType.Int)
      }
      // o-z
      return TypedToken(TokenType.Variable, first.toString(), VarType.Str)
    }
    var maybeKeyword = first.toString()
    while (Character.isLetter(cc) && cc != ZERO) {
      maybeKeyword += cc.toString()
      advance()
    }
    val kw = KeywordType.valueOf(maybeKeyword.uppercase());
    return KeywordToken(maybeKeyword, kw)
  }

  private fun makeNumber(): Token {
    val first = cc
    advance()
    var num = first.toString()
    while (Character.isDigit(cc) && cc != ZERO) {
      num += cc
      advance()
    }
    if (cc != '.') {
      // Not a float.
      return TypedToken(TokenType.Constant, num, VarType.Int)
    }
    num += cc
    advance()
    while (Character.isDigit(cc) && cc != ZERO) {
      num += cc
      advance()
    }
    return TypedToken(TokenType.Constant, num, VarType.Float)
  }
}
