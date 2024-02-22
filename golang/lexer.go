package main

import (
    "fmt"
    "strings"
)

type Lexer struct {
  text string
  loc int
  cc byte // naive ascii
}


func NewLexer(text string) Lexer {
  lexer := Lexer{text: text, loc: 0}
  lexer.advance()
  return lexer
}

func (this Lexer) String() string {
  return fmt.Sprintf("{Lexer \"%s\" 0x%x @ %d}", this.text, this.cc, this.loc)
}

func (this *Lexer) NextToken() Token {
  for {
    for this.cc == ' ' || this.cc == '\n' || this.cc == '\t' || this.cc == '\r' {
      this.advance()
    }
    if this.cc != '#' {
      break
    }
    for this.cc != '\n' && this.cc != 0 {
      this.advance()
    }
  }
  if this.cc == 0 {
  return NewToken(EndOfFile, "")
  }
  if isalpha(this.cc) {
    return this.makeText() // private
  }
  if isdigit(this.cc) {
    return this.makeNumber()
  }
  if this.cc == '"' {
    return this.makeStringLiteral()
  }
  return this.makeSymbol()
}

func (this *Lexer) advance() {
  if this.loc < len(this.text) {
    this.cc = this.text[this.loc]
    this.loc++
  } else {
    this.cc = 0
  }
}

func (this *Lexer) makeSymbol() Token {
  first := this.cc
  value := string(first)
  this.advance()
  if this.cc == '=' && (first == '!' || first == '=' || first == '>' || first == '<') {
    value += string(this.cc)
    this.advance()
  }
  symbolType, ok := toSymbolType[value]
  if ok {
    return Token{tokenType: Symbol, value: value, symbol: symbolType}
  }
  panic("Cannot parse symbol " + value)
}


func (this *Lexer) makeText() Token {
  first := this.cc
  value := string(first)
  this.advance()
  if !isalpha(this.cc) {
    // next char is not alpha; it's a variable.
    varType := StrVarType
    switch {
      case (first >= 'a' && first <= 'h') || (first >= 'A' && first <= 'H'):
        varType = FloatVarType
      case (first >= 'i' && first <= 'n') || (first >= 'I' && first <= 'N'):
        varType = IntVarType
      default:
        varType = StrVarType
    }

    return Token{tokenType: Var, value: value, varType: varType}
  }

  for isalpha(this.cc) && this.cc != 0 {
    value += string(this.cc)
    this.advance()
  }
  value = strings.ToUpper(value)
  kw, ok := toKeywordType[value]
  if ok {
    return Token{tokenType: Keyword, value: value, keyword: kw}
  }
  panic("Unknown keyword " + value)
}

func (this *Lexer) makeNumber() Token {
  value := string(this.cc)
  this.advance()
  for isdigit(this.cc) && this.cc != 0 {
    value += string(this.cc)
    this.advance()
  }
  if this.cc == '.' {
    // Float
    value += string(this.cc)
    this.advance()
    for isdigit(this.cc) && this.cc != 0 {
      value += string(this.cc)
      this.advance()
    }
    return Token{tokenType: Const, value: value, varType: FloatVarType}
  }
  return Token{tokenType: Const, value: value, varType: IntVarType}
}

func (this *Lexer) makeStringLiteral() Token {
  this.advance() // eat the starting quote
  value := ""
  for this.cc != '"' && this.cc != 0 {
    value += string(this.cc)
    this.advance()
  }
  if this.cc == 0 {
    panic("Unclosed string literal")
  }
  this.advance() // eat the ending quote
  return Token{tokenType: Const, value: value, varType: StrVarType}
}

func isalpha(c byte) bool {
  return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
}

func isdigit(c byte) bool {
  return c >= '0' && c <= '9'
}
