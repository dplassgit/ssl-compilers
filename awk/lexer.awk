BEGIN {
  FS = "" # input field separator
  ORS=" " # output record separator 

  ev = ""

  loc = 0
  cc = -1

  # Token types
  EOF = "eof"
  CONST = "const"
  KEYWORD = "keyword"
  SYMBOL = "symbol"
  VAR = "var"

  FLOAT = "float"
  INT = "int"
  STRING = "string"

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
}

function skipWhitespace() {
  while (1) {
    while (loc < length(ev) && (cc == " " || cc == "\n")) {
      advance()
    }
    if (loc == length(ev)) {
      return
    }
    if (cc == "#") {
      advance()
      while (loc < length(ev) && cc != "\n") {
        advance()
      }
    } 
    return
  }
}

function advance() {
  loc = loc + 1
  cc = -1 
  if (loc < length(ev)) {
    cc = substr(ev, loc, 1)
  }
}

function isAlpha(c) {
  return match(c, /^[a-zA-Z]/)
}

function isNumber(c) {
  return match(c, /[0-9]/)
}

# Token is an array: first element is the token type, second element is the string, third element is the vartype
function nextToken(token) {
  if (loc == length(ev)) {
    token[0] = EOF
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
    print "FAIL - unclosed string constant"
    token[0] = EOF
    token[1] = soFar
    token[2] = STRING
    return
  }
  advance() # eat the closing quote
  token[0] = CONST
  token[1] = soFar
  token[2] = STRING
}

function makeSymbol(token) {
  # TODO: get real symbol
  token[0] = SYMBOL
  token[1] = cc
  advance()
}

function makeNumber(token) {
  soFar = cc
  advance()
  while (cc != -1 && isNumber(cc)) {
    soFar = soFar cc
    advance()
  }
  token[0] = CONST
  token[1] = soFar
  token[2] = INT
}

function makeTextToken(token) {
  first = cc
  advance()
  if (!isAlpha(cc)) {
    # got a variable
    token[0] = VAR
    token[1] = first
    token[2] = INT
  } else {
    # Keyword
    soFar = first
    while (cc != -1 && isAlpha(cc)) {
      soFar = soFar cc
      advance()
    }
    if (soFar in keywords) {
      token[0] = KEYWORD
      token[1] = soFar
    } else {
      token[0] = EOF
      token[1] = "Unknown keyword " soFar
    }
  }
}

