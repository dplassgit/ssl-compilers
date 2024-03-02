require "test/unit"
require_relative "parser"


class TestLexer< Test::Unit::TestCase
  def test_empty
    parser = Parser.new("")
    code = parser.parse()
    assert_equal(code.length, 6)
  end

  def test_print_string
    parser = Parser.new('println "hi"')
    code = parser.parse()
    assert_contains(code, "  extern printf")
  end

  def test_print_int
    parser = Parser.new('println 314')
    code = parser.parse()
    assert_contains(code, "  extern printf")
    assert_contains(code, "  INT_FMT: db '%d', 0")
  end

  def test_print_float
    parser = Parser.new('println 3.14')
    code = parser.parse()
    assert_contains(code, "  extern printf")
    assert_contains(code, "  FLOAT_FMT: db '%.16g', 0")
    assert_contains(code, "  FLOAT_1: dq 3.14")
  end

  def test_print_bool
    parser = Parser.new("println 3==3")
    code = parser.parse()
    assert_contains(code, "  cmp EBX, EAX")
    assert_contains(code, "  setz AL")
    assert_contains(code, "  extern printf")
    assert_contains(code, "  TRUE: db 'true', 0")
    assert_contains(code, "  FALSE: db 'false', 0")
  end

  def test_print_float_lt_bool
    parser = Parser.new("println 3.0<4.0")
    code = parser.parse()
    assert_contains(code, "  setb AL")
  end

  def test_print_float_le_bool
    parser = Parser.new("println 3.0<=4.0")
    code = parser.parse()
    assert_contains(code, "  setbe AL")
  end

  def test_print_float_gt_bool
    parser = Parser.new("println 3.0>4.0")
    code = parser.parse()
    assert_contains(code, "  seta AL")
  end

  def test_print_float_ge_bool
    parser = Parser.new("println 3.0>=4.0")
    code = parser.parse()
    assert_contains(code, "  setae AL")
  end

  def test_assign_int
    parser = Parser.new('i=3')
    code = parser.parse()
    assert_contains(code, "  mov EAX, 3")
    assert_contains(code, "  mov [_i], EAX")
    assert_contains(code, "  _i: dd 0")
  end

  def test_assign_string
    parser = Parser.new('s="hi"')
    code = parser.parse()
    assert_contains(code, "  mov [_s], RAX")
    assert_contains(code, "  _s: dq 0")
  end

  def test_assign_var
    parser = Parser.new('i=3 j=i')
    code = parser.parse()
    assert_contains(code, "  mov EAX, [_i]")
    assert_contains(code, "  mov [_j], EAX")
    assert_contains(code, "  _i: dd 0")
    assert_contains(code, "  _j: dd 0")
  end

  def test_print_int_var
    parser = Parser.new('i=3 println i')
    code = parser.parse()
    assert_contains(code, "  mov [_i], EAX")
    assert_contains(code, "  extern printf")
  end

  def test_print_float_var
    parser = Parser.new('a=3.0 println a')
    code = parser.parse()
    assert_contains(code, "  movq [_a], XMM0")
    assert_contains(code, "  extern printf")
  end

  def test_print_string_var
    parser = Parser.new('s="hi" println s')
    code = parser.parse()
    assert_contains(code, "  mov [_s], RAX")
    assert_contains(code, "  mov RCX, RAX")
    assert_contains(code, "  extern printf")
  end

  def test_add_int_constants
    parser = Parser.new('i=1+1')
    code = parser.parse()
    assert_contains(code, "  mov EAX, 1")
    assert_contains(code, "  add EAX, EBX")
    assert_contains(code, "  mov [_i], EAX")
  end

  def test_mult_int_constants
    parser = Parser.new('i=2*4')
    code = parser.parse()
    assert_contains(code, "  imul EAX, EBX")
  end

  def test_sub_int_constants
    parser = Parser.new('i=4-2')
    code = parser.parse()
    assert_contains(code, "  push RAX")
    assert_contains(code, "  pop RBX")
    assert_contains(code, "  xchg EAX, EBX")
    assert_contains(code, "  sub EAX, EBX")
  end

  def test_div_int_constants
    parser = Parser.new('i=4/2')
    code = parser.parse()
    assert_contains(code, "  push RAX")
    assert_contains(code, "  pop RBX")
    assert_contains(code, "  xchg EAX, EBX")
    assert_contains(code, "  cdq")
    assert_contains(code, "  idiv EBX")
  end

  def test_add_int_vars
    parser = Parser.new('i=1 j=2 k=i+j')
    code = parser.parse()
    assert_contains(code, "  mov EAX, [_i]")
    assert_contains(code, "  push RAX")
    assert_contains(code, "  mov EAX, [_j]")
    assert_contains(code, "  pop RBX")
    assert_contains(code, "  add EAX, EBX")
    assert_contains(code, "  mov [_k], EAX")
  end

  def test_div_float_constants
    parser = Parser.new('a=4.0/2.0')
    code = parser.parse()
    assert_contains(code, "  divsd XMM1, XMM0")
    assert_contains(code, "  movq XMM0, XMM1")
  end

  def test_add_float_constants
    parser = Parser.new('a=4.0+2.0')
    code = parser.parse()
    assert_contains(code, "  addsd XMM0, XMM1")
  end

  def test_if
    parser = Parser.new('if 1==2 then println "yes" endif')
    code = parser.parse()
    assert_contains(code, "else_1:")
    assert_not_contains(code, "endif_2:")
  end

  def test_if_else
    parser = Parser.new('if 1==2 then println "yes" else println "no" endif')
    code = parser.parse()
    assert_contains(code, "else_1:")
    assert_contains(code, "endif_2:")
  end

  def test_for
    parser = Parser.new('for i = 1 to 10 println i endfor')
    code = parser.parse()
    assert_contains(code, "  inc DWORD [_i]")
    assert_contains(code, "  _i: dd 0")
  end

  def test_for_bad_type
    parser = Parser.new('for a = 1 to 10 println a endfor')
    begin
      parser.parse()
      fail "Expected failure"
    rescue
    end
  end

  def assert_contains(code, line)
    assert_equal(code.include?(line), true)
  end

  def assert_not_contains(code, line)
    assert_equal(code.include?(line), false)
  end
end

