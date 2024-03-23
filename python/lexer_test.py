from ssl_token import *
from lexer import Lexer

import unittest


class LexerTest(unittest.TestCase):
    def test_empty(self):
        lexer = Lexer("")
        token = lexer.nextToken()
        self.assertTrue(token.isEof())

    def test_comment(self):
        lexer = Lexer("# nothing\n")
        token = lexer.nextToken()
        self.assertTrue(token.isEof())

    def test_skips_comments(self):
        lexer = Lexer('"hi" # comment\n123\n')
        token = lexer.nextToken()
        self.assertEqual(token.tokenType, TokenType.CONST)
        self.assertEqual(token.varType, VarType.STR)
        self.assertEqual(token.value, "hi")
        token = lexer.nextToken()
        self.assertEqual(token.tokenType, TokenType.CONST)
        self.assertEqual(token.varType, VarType.INT)
        self.assertEqual(token.value, 123)
        token = lexer.nextToken()
        self.assertTrue(token.isEof())

    def test_number(self):
        lexer = Lexer("123")
        token = lexer.nextToken()
        self.assertEqual(token.tokenType, TokenType.CONST)
        self.assertEqual(token.varType, VarType.INT)
        self.assertEqual(token.value, 123)

    def test_float(self):
        lexer = Lexer("123.456")
        token = lexer.nextToken()
        self.assertEqual(token.tokenType, TokenType.CONST)
        self.assertEqual(token.varType, VarType.FLOAT)
        self.assertEqual(token.value, 123.456)

    def test_string_constant(self):
        lexer = Lexer('"123"')
        token = lexer.nextToken()
        self.assertEqual(token.tokenType, TokenType.CONST)
        self.assertEqual(token.varType, VarType.STR)
        self.assertEqual(token.value, "123")

    def test_variables(self):
        expected = [("a", VarType.FLOAT), ("i", VarType.INT),
                    ("s", VarType.STR), ("z", VarType.STR)]
        program = " ".join(value for (value, varType) in expected)
        lexer = Lexer(program)
        for (value, varType) in expected:
            token = lexer.nextToken()
            self.assertEqual(token.tokenType, TokenType.VAR)
            self.assertEqual(token.value, value)
            self.assertEqual(token.varType, varType)

    def test_upper_variables(self):
        expected = [("A", VarType.FLOAT), ("I", VarType.INT),
                    ("S", VarType.STR), ("Z", VarType.STR)]
        program = " ".join(value for (value, varType) in expected)
        lexer = Lexer(program)
        for (value, varType) in expected:
            token = lexer.nextToken()
            self.assertEqual(token.tokenType, TokenType.VAR)
            self.assertEqual(token.value, value)
            self.assertEqual(token.varType, varType)

    def test_keywords(self):
        lexer = Lexer("if then else endif for to step endfor print println")
        expected = [Keyword.IF, Keyword.THEN, Keyword.ELSE, Keyword.ENDIF,
                    Keyword.FOR, Keyword.TO, Keyword.STEP, Keyword.ENDFOR,
                    Keyword.PRINT, Keyword.PRINTLN]
        for keyword in expected:
            token = lexer.nextToken()
            self.assertEqual(token.tokenType, TokenType.KEYWORD)
            self.assertEqual(token.value, keyword)

    def test_upper_keywords(self):
        lexer = Lexer("IF THEN ELSE ENDIF FOR TO STEP ENDFOR PRINT PRINTLN")
        expected = [Keyword.IF, Keyword.THEN, Keyword.ELSE, Keyword.ENDIF,
                    Keyword.FOR, Keyword.TO, Keyword.STEP, Keyword.ENDFOR,
                    Keyword.PRINT, Keyword.PRINTLN]
        for keyword in expected:
            token = lexer.nextToken()
            self.assertEqual(token.tokenType, TokenType.KEYWORD)
            self.assertEqual(token.value, keyword)

    def test_symbols(self):
        symbols = ["+", "-", "*", "/", "=", "<", "<=", "==", "!=", ">=", ">"]
        program = " ".join(symbols)
        lexer = Lexer(program)
        for symbol in symbols:
            token = lexer.nextToken()
            self.assertEqual(token.tokenType, TokenType.SYMBOL)
            self.assertEqual(token.value, symbol)

    def test_symbols_no_spaces(self):
        expected = ["+", "-", "*", "/", "=", "<", "<=", "==", "!=", ">=", ">"]
        lexer = Lexer("+-*/=<<===!=>=>")
        for symbol in expected:
            token = lexer.nextToken()
            self.assertEqual(token.tokenType, TokenType.SYMBOL)
            self.assertEqual(token.value, symbol)


if __name__ == "__main__":
    unittest.main()
