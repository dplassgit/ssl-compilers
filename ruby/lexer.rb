require_relative "token"

class Lexer
  def initialize(text)
    @text = text
    @loc = 0
    advance()
  end

  def next_token
    while true
      while (not @cc.nil?) and @cc <= ' '
        advance()
      end
      if @cc != '#'
        break
      end
      # NOTE must use double quotes for backslash, sheesh. IHR
      while (not @cc.nil?) and @cc != "\n"
        advance()
      end
    end
    if @cc.nil?
      return Token.new(type: :eof)
    end
    if @cc =~ /[[:alpha:]]/
      return make_text()
    end
    if @cc =~ /[[:digit:]]/
      return make_number()
    end
    if @cc == '"'
      return make_string()
    end
    return make_symbol()
  end

  private 

  def make_symbol
    first = @cc
    val = first
    advance()
    if ['=', '<', '>', '!'].include?(first)
      # equals can follow
      if not @cc.nil? and @cc == '='
        val += '='
        advance()
      end
    end
    index = SYMBOL_STRINGS.index(val)
    if index.nil?
      fail("Unknown symbol #{val}")
    end
    sym = SYMBOLS[index]
    return Token.new(type: TokenType::Symbol, value: val, symbol: sym)
  end

  def advance
    @cc = @text[@loc]
    @loc = @loc + 1
  end

  def make_string
    advance()   # eat the opening quote
    val = ""
    while not @cc.nil? and @cc != '"'
      val += @cc
      advance()
    end
    if @cc.nil?
      fail "Unclosed string literal #{val}"
    end
    advance()
    return Token.new(type: TokenType::Constant, value: val, var_type: :string)
  end

  def make_number
    val = @cc
    advance()
    while not @cc.nil? and @cc =~ /[[:digit:]]/
      val += @cc
      advance()
    end
    if @cc != '.'
      return Token.new(type: TokenType::Constant, value: val, var_type: :int)
    end
    val += @cc
    advance()
    while not @cc.nil? and @cc =~ /[[:digit:]]/
      val += @cc
      advance()
    end
    return Token.new(type: TokenType::Constant, value: val, var_type: :float)
  end

  def make_text
    var = @cc
    advance()
    if not (@cc =~ /[[:alpha:]]/)
      lowervar = var.downcase #ihr
      if lowervar >= 'a' and lowervar <= 'h'
        return Token.new(type: TokenType::Variable, value: var, var_type: :float)
      elsif lowervar <= 'n' 
        return Token.new(type: TokenType::Variable, value: var, var_type: :int)
      else
        return Token.new(type: TokenType::Variable, value: var, var_type: :string)
      end
    end
    while not @cc.nil? and @cc =~ /[[:alpha:]]/
      var += @cc
      advance()
    end
    index = KW_STRINGS.index(var.upcase)
    if index != nil
      return Token.new(type: TokenType::Keyword, value: var, keyword: KEYWORDS[index])
    else
      fail("Unknown keyword " + var)
    end
  end
end

