require "test/unit"

require_relative "lexer"

class TestLexer< Test::Unit::TestCase
  def test_comment
    lexer = Lexer.new("a #comment \n b")
    t = lexer.next_token
    assert_equal(t.value, "a")
    t = lexer.next_token
    assert_equal(t.value, "b")
  end

  def test_next_token_float_variable
    ("a".."h").each do |var|
      t = Lexer.new(var).next_token
      assert_equal(t.type, TokenType::Variable)
      assert_equal(t.value, var)
      assert_equal(t.var_type, VarType::Float)
    end
  end

  def test_next_token_int_variable
    ("i".."n").each do |var|
      t = Lexer.new(var).next_token
      assert_equal(t.type, TokenType::Variable)
      assert_equal(t.value, var)
      assert_equal(t.var_type, VarType::Int)
    end
  end

  def test_next_token_string_variable
    ("s".."z").each do |var|
      t = Lexer.new(var).next_token
      assert_equal(t.type, TokenType::Variable)
      assert_equal(t.value, var)
      assert_equal(t.var_type, VarType::String)
    end
  end

  def test_next_token_keyword
    KEYWORDS.each do |kw|
      t = Lexer.new(kw.to_s).next_token
      assert_equal(t.type, TokenType::Keyword)
      assert_equal(t.keyword, kw)
    end
  end

  def test_next_token_int_constant
    lexer = Lexer.new("1 234")
    t = lexer.next_token
    assert_equal(t.type, TokenType::Constant)
    assert_equal(t.value, "1")
    assert_equal(t.var_type, VarType::Int)
    t = lexer.next_token
    assert_equal(t.type, TokenType::Constant)
    assert_equal(t.value, "234")
    assert_equal(t.var_type, VarType::Int)
  end

  def test_next_token_float_constant
    lexer = Lexer.new("1.0 234.5")
    t = lexer.next_token
    assert_equal(t.type, TokenType::Constant)
    assert_equal(t.value, "1.0")
    assert_equal(t.var_type, VarType::Float)
    t = lexer.next_token
    assert_equal(t.type, TokenType::Constant)
    assert_equal(t.value, "234.5")
    assert_equal(t.var_type, VarType::Float)
  end

  def test_next_token_string_constant
    lexer = Lexer.new('"a" "abcde"')
    t = lexer.next_token
    assert_equal(t.type, TokenType::Constant)
    assert_equal(t.value, "a")
    assert_equal(t.var_type, VarType::String)
    t = lexer.next_token
    assert_equal(t.type, TokenType::Constant)
    assert_equal(t.value, "abcde")
    assert_equal(t.var_type, VarType::String)
  end

  def test_next_token_symbol
    lexer = Lexer.new(SYMBOL_STRINGS.join(' '))
    for i in 0..10
      t = lexer.next_token
      assert_equal(t.type, TokenType::Symbol)
      assert_equal(t.value, SYMBOL_STRINGS[i])
      assert_equal(t.symbol, SYMBOLS[i])
    end
  end
end

