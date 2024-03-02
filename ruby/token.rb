class TokenType
  Eof=:eof
  Variable=:variable
  Constant=:constant
  Keyword=:keyword
  Symbol=:symbol
end

# Not sure why this works, but I couldn't get :constant to work >:-(
KEYWORDS=[:if, :then, :else, :endif, :for, :to, :step, :endfor, :print, :println]
KW_STRINGS = KEYWORDS.map{|k| k.to_s.upcase}

class VarType
  Int=:int
  Float=:float
  String=:string
  Bool=:bool
end

SYMBOLS=[:plus, :minus, :mult, :div, :eq_eq, :neq, :lt, :leq, :gt, :geq, :eq]
SYMBOL_STRINGS = ['+', '-', '*', '/', '==', '!=', '<', '<=', '>', '>=', '=']

class Token
  attr_reader :type, :value, :keyword, :var_type, :symbol

  def initialize(type:, value: "", keyword: nil, var_type: nil, symbol: nil)
    # TODO: make sure type is in the list
    @type = type
    @value = value
    @keyword = keyword
    @var_type = var_type
    @symbol = symbol
  end

  def is_keyword?(kw)
    @type == :keyword and @keyword == kw
  end
end

