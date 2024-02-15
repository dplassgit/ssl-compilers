# Get input
# tokenize
# Recurse
# Output

# Token types
INT_CONST=1
INT_VAR=2
FLOAT_CONST=3
FLOAT_VAR=4
STR_CONST=5
STR_VAR=6
KEYWORD=7
SYMBOL=8

# Keyword types
IF=1
THEN=2
ELSE=3
ENDIF=4
FOR=5
TO=6
STEP=7
ENDFOR=8
PRINT=9
PRINTLN=10
EOF=999
KEYWORDS = {
    IF: 'if',
    THEN: 'then',
    ELSE: 'else',
    ENDIF: 'endif',
    FOR: 'for',
    TO: 'to',
    STEP: 'step',
    ENDFOR: 'endfor',
    PRINT: 'print',
    PRINTLN: 'println'
}


class Lexer:
    def __init__(self, text):
        self.text = text
        self.advance()

    def advance(self):
        pass

    # Token is a tuple: (type, value)
    def nextToken(self):
        return (KEYWORD, 'if')


class Parser:
    def __init__(self, text):
        self.lexer = Lexer(text)
        self.advance()

    def advance(self):
        self.token = self.lexer.nextToken()
        print(self.token[1])
        return self.token

    def parse(self):
        pass


def main():
    input = 'println 3'
    p = Parser(input)
    p.parse()


if __name__ == '__main__':
    main()
