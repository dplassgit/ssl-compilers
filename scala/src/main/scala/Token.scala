package com.plasstech.lang.ssl

enum TokenType:
  case EndOfFile, Variable, Constant, Keyword, Symbol

enum KeywordType:
  case Print, Println, If, Then, Else, Endif, For, To, Next, Endfor, NoKeyword

enum SymbolType(t: String):
  case Eq extends SymbolType("=")
  case EqEq extends SymbolType("==")
  case Neq extends SymbolType("!=")
  case Lt extends SymbolType("<")
  case Leq extends SymbolType("<=")
  case Gt extends SymbolType(">")
  case Geq extends SymbolType(">=")
  case Plus extends SymbolType("+")
  case Minus extends SymbolType("-")
  case Mult extends SymbolType("*")
  case Div extends SymbolType("/")
  case NoSymbol extends SymbolType("")
  def text(): String = t

enum VarType:
  case VarTypeInt, VarTypeFloat, VarTypeString, VarTypeBool, NoVarType

class Token(
  private val _tokenType: TokenType,
  private val _value: String = "",
  private val _keyword: KeywordType = KeywordType.NoKeyword,
  private val _symbol: SymbolType = SymbolType.NoSymbol,
  private val _varType: VarType = VarType.NoVarType
):

  def this(kw: KeywordType) = this(TokenType.Keyword, _keyword=kw)
  def this(st: SymbolType) = this(TokenType.Symbol, _symbol=st)

  override def toString =
    _tokenType match
      case TokenType.EndOfFile => s"type: $_tokenType"
      case TokenType.Keyword => s"type: $_tokenType, keyword: $_keyword"
      case TokenType.Variable | TokenType.Constant =>
          s"type: $_tokenType, value: $_value, varType: $_varType"
      case TokenType.Symbol => s"type: $_tokenType, symbol: $_symbol"

  def tokenType(): TokenType = _tokenType
  def value(): String = _value
  // TODO: if not the right type, throw an exception
  def keyword(): KeywordType = _keyword
  def symbolType(): SymbolType = _symbol
  def varType(): VarType = _varType

  def isKeyword(kw: KeywordType) =
    _tokenType == TokenType.Keyword && _keyword == kw

end Token

def newVariable(name: String, vt: VarType): Token =
    new Token(TokenType.Variable, name, _varType=vt)
def newConstant(value: String, vt: VarType): Token =
    new Token(TokenType.Constant, value, _varType=vt)
