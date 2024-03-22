package main
import "testing"

func TestNumber(t *testing.T) {
  lexer := NewLexer("123")
  token := lexer.NextToken()
  if token.tokenType != Const {
    t.Errorf("Parsing 123 got: %v, want: %v.", token, Const)
  }
}

func TestSymbols(t *testing.T) {
  lexer := NewLexer("<> =>=<=!===*+/-")
  expected := []SymbolType{
    Lt,
    Gt,
    Eq,
    Geq,
    Leq,
    Neq,
    EqEq,
    Mult,
    Plus,
    Div,
    Minus,
  }

  for _, symbol := range(expected) {
    token := lexer.NextToken()
    if token.symbol != symbol {
      t.Errorf("got: %v, want: %v.",
          fromSymbolType[token.symbol], fromSymbolType[symbol])
    }
  }
}
