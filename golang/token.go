package main

type TokenType int
const (
  EndOfFile TokenType = iota + 1
  Const
  Var
  Keyword
  Symbol
)

type KeywordType int // cannot be "Keyword" because of the above const.
const (
  If KeywordType = iota + 1
  Then
  Else
  EndIf
  For
  To
  Step
  EndFor
  Print
  PrintLn
  NoKeyword = -1
)

var toKeywordType = map[string]KeywordType {
  "IF": If,
  "THEN": Then,
  "ELSE": Else,
  "ENDIF": EndIf,
  "FOR": For,
  "TO": To,
  "STEP": Step,
  "ENDFOR": EndFor,
  "PRINT": Print,
  "PRINTLN": PrintLn,
}

type VarType int
const (
  IntVarType VarType = iota + 1
  StrVarType
  BoolVarType
  FloatVarType
  NoVarType = -1
)

var varTypeToString = map[VarType]string {
  IntVarType: "INT",
  StrVarType: "STR",
  BoolVarType: "BOOL",
  FloatVarType: "FLOAT",
}

type SymbolType int
const (
  Plus SymbolType = iota + 1
  Minus
  Mult
  Div
  EqEq
  Eq
  Neq
  Leq
  Lt
  Geq
  Gt
  NoSymbolType = -1
)

var toSymbolType = map[string]SymbolType {
  "+": Plus,
  "-": Minus,
  "*": Mult,
  "/": Div,
  "==": EqEq,
  "=": Eq,
  "!=": Neq,
  "<=": Leq,
  "<": Lt,
  ">=": Geq,
  ">": Gt,
}

var fromSymbolType = map[SymbolType]string {
  Plus: "+",
  Minus: "-",
  Mult: "*",
  Div: "/",
  EqEq: "==",
  Eq: "=",
  Neq: "!=",
  Leq: "<=",
  Lt: "<",
  Geq: ">=",
  Gt: ">",
}

type Token struct {
  tokenType TokenType
  value string
  keyword KeywordType
  varType VarType
  symbol SymbolType
}

// constructor
func NewToken(tokenType TokenType, value string) Token {
  return Token{
      tokenType: tokenType,
      value: value,
      keyword: NoKeyword,
      varType: NoVarType,
      symbol: NoSymbolType,
  }
}

func (this Token) IsKeyword(kw KeywordType) bool {
  return this.tokenType == Keyword && this.keyword == kw
}
