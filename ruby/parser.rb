require_relative "lexer"
require_relative "token"

INT_OPCODES = {
  :plus => ["add EAX, EBX"],
  :mult =>[ "imul EAX, EBX"],
  :div => ["xchg EAX, EBX", "cdq", "idiv EBX"],
  :minus => ["xchg EAX, EBX", "sub EAX, EBX"],
  :eq_eq => ["cmp EBX, EAX", "setz AL"],
  :neq => ["cmp EBX, EAX", "setnz AL"],
  :lt => ["cmp EBX, EAX", "setl AL"],
  :gt => ["cmp EBX, EAX", "setg AL"],
  :geq => ["cmp EBX, EAX", "setge AL"],
  :leq => ["cmp EBX, EAX", "setle AL"],
}
FLOAT_OPCODES = {
  :plus => ["addsd XMM0, XMM1"],
  :mult =>[ "mulsd XMM0, XMM1"],
  :div => ["divsd XMM1, XMM0", "movq XMM0, XMM1"],
  :minus => ["subsd XMM1, XMM0", "movq XMM0, XMM1"],
  :eq_eq => ["comisd XMM1, XMM0", "setz AL"],
  :neq => ["comisd XMM1, XMM0", "setnz AL"],
  :lt => ["comisd XMM1, XMM0", "setb AL"],
  :gt => ["comisd XMM1, XMM0", "seta AL"],
  :leq => ["comisd XMM1, XMM0", "setbe AL"],
  :geq => ["comisd XMM1, XMM0", "setae AL"],
}
COMP_SYMBOLS = [:eq_eq, :neq, :lt, :leq, :gt, :geq]

class Parser
  def initialize(text)
    @lexer = Lexer.new(text)
    @token = nil
    @code = []
    @data = []
    @string_table = {}
    @float_table = {}
    @id = 0
  end

  def parse()
    advance()
    emit0("global main")
    emit0("section .text")
    emit0("main:")
    statements()
    emit("extern exit")
    emit("call exit")
    emit("")
    emit0("section .data") if @data.length > 0
    @data.each { |line| emit(line) }
    return @code
  end

  private

  def statements()
    while @token.type != :eof
      statement()
    end
  end

  def statement()
    if @token.type == TokenType::Keyword
      if @token.keyword == :print or @token.keyword == :println
        parse_print()
        return
      elsif @token.keyword == :if
        parse_if()
        return
      elsif @token.keyword == :for
        parse_for()
        return
      end
    end
    if @token.type == TokenType::Variable
      parse_assignment()
      return
    end
    fail "Cannot parse #{@token.value} yet"
  end

  def parse_if()
    expect_keyword(:if)
    expr_type = expr()
    check_types(VarType::Bool, expr_type)
    else_label = next_label("else")
    endif_label = next_label("endif")
    emit("cmp AL, 0x01")
    emit("jne #{else_label}")

    expect_keyword(:then)
    while (not @token.is_keyword?(:endif)) and
          (not @token.is_keyword?(:else)) and
          @token.type != TokenType::Eof
      statement()
    end
    if @token.type == TokenType::Eof
      fail "Expected ELSE or EOF, saw EOF"
    end
    has_else = @token.is_keyword?(:else)
    if has_else
      emit("jmp #{endif_label}")
    end
    emit_label(else_label)
    if has_else
      advance() # eat the else (we didn't use expect_keyword)
      while (not @token.is_keyword?(:endif)) and
          (not @token.type == TokenType::Eof)
        statement()
      end
    end
    expect_keyword(:endif)
    if has_else
      emit_label(endif_label)
    end
  end

  def parse_for()
    expect_keyword(:for)
    if @token.type != TokenType::Variable
      fail "Expected variable, saw #{@token.type}"
    end
    check_types(VarType::Int, @token.var_type)
    var_name = @token.value
    add_data("_#{var_name}: dd 0")
    advance()

    expect_symbol('=')

    from_type = expr()
    check_types(VarType::Int, from_type)
    emit("mov [_#{var_name}], EAX")

    expect_keyword(:to)

    for_label = next_label("for")
    endfor_label = next_label("endfor")
    emit_label(for_label)

    to_type = expr()
    check_types(VarType::Int, to_type)

    emit("cmp [_#{var_name}], EAX")
    emit("jge #{endfor_label}")

    while (not @token.is_keyword?(:endfor)) and
          @token.type != TokenType::Eof
      statement()
    end

    expect_keyword(:endfor)
    emit("inc DWORD [_#{var_name}]")
    emit("jmp #{for_label}")
    emit_label(endfor_label)
  end

  def parse_assignment()
    var = @token.value
    var_type = @token.var_type
    case @token.var_type
      when VarType::String then add_data("_#{var}: dq 0")
      when VarType::Float then add_data("_#{var}: dq 0.0")
      when VarType::Int then add_data("_#{var}: dd 0")
      else fail "Cannot assign to type #{@token.var_type}"
    end
    advance() # eat the variable
    expect_symbol('=')
    expr_type = expr()
    check_types(var_type, expr_type)
    case var_type
      when VarType::String then emit("mov [_#{var}], RAX")
      when VarType::Float then emit("movq [_#{var}], XMM0")
      when VarType::Int then emit("mov [_#{var}], EAX")
      else fail "Cannot assign to type #{var_type}"
    end
  end

  def check_types(left, right)
    if left != right
      fail "Incompatible types: expected #{left}, was #{right}"
    end
  end

  def expect_symbol(symbol_string)
    if @token.type != :symbol or @token.value != symbol_string
      fail "Expected #{symbol_string}, was #{@token.value}"
    end
    advance()
  end

  def expect_keyword(kw)
    if @token.type != :keyword or @token.keyword != kw
      fail "Expected #{kw}, was #{@token.value}"
    end
    advance()
  end

  def parse_print()
    is_println = @token.keyword == :println
    advance() # eat the keyword
    expr_type = expr()
    case expr_type
      when VarType::String then emit("mov RCX, RAX")
      when VarType::Int
        add_data("INT_FMT: db '%d', 0")
        emit("mov RCX, INT_FMT")
        emit("mov EDX, EAX")
      when VarType::Float
        add_data("FLOAT_FMT: db '%.16g', 0")
        emit("mov RCX, FLOAT_FMT")
        emit("movq RDX, XMM0")
      when VarType::Bool
        add_data("TRUE: db 'true', 0")
        add_data("FALSE: db 'false', 0")
        emit("cmp AL, 1")
        emit("mov RCX, FALSE")
        emit("mov RDX, TRUE")
        emit("cmovz RCX, RDX")
      else fail "Cannot print #{expr_type} yet"
    end
    emit("sub RSP, 0x20")
    emit("extern printf")
    emit("call printf")
    if is_println
      emit("extern putchar")
      emit("mov RCX, 10")
      emit("call putchar")
    end
    emit("add RSP, 0x20")
  end

  def expr()
    left_type = atom()
    if @token.type == TokenType::Symbol
      op = @token.symbol
      advance()
      case left_type
        when VarType::Int then emit("push RAX")
        when VarType::Float
          emit("sub RSP, 0x08")
          emit("movq [RSP], XMM0")
        else fail "Cannot push #{left_type} yet"
      end
      right_type = atom()
      check_types(left_type, right_type)
      case left_type
        when VarType::Int
          emit("pop RBX")
          opcodes = INT_OPCODES[op]
          if opcodes.nil?
            fail "Cannot codegen #{op} yet"
          end
          opcodes.each { |opcode| emit(opcode) }

        when VarType::Float
          emit("movq XMM1, [RSP]")
          emit("add RSP, 0x08")
          opcodes = FLOAT_OPCODES[op]
          if opcodes.nil?
            fail "Cannot codegen #{op} yet"
          end
          opcodes.each { |opcode| emit(opcode) }

        else fail "Cannot pop #{left_type} yet"
      end
      if COMP_SYMBOLS.include?(op)
        return VarType::Bool
      end
    end
    return left_type
  end

  def atom()
    case @token.type
      when TokenType::Constant
        case @token.var_type
          when VarType::String
            name = make_string_constant(@token.value)
            emit("mov RAX, #{name}")
            advance()
            return VarType::String

          when VarType::Float
            name = make_float_constant(@token.value)
            emit("movq XMM0, [#{name}]")
            advance()
            return VarType::Float

          when VarType::Int
            emit("mov EAX, #{@token.value}")
            advance()
            return VarType::Int
        end
      when TokenType::Variable
        case @token.var_type
          when VarType::String
            emit("mov RAX, [_#{@token.value}]")
            advance()
            return VarType::String

          when VarType::Float
            emit("movq XMM0, [_#{@token.value}]")
            advance()
            return VarType::Float

          when VarType::Int
            emit("mov EAX, [_#{@token.value}]")
            advance()
            return VarType::Int
        end
    end
    fail "Cannot codegen #{@token.type}: (#{@token.value}, #{@token.var_type}) yet"
  end

  def make_string_constant(value)
    name = @string_table[value]
    if not name.nil?
      return name
    end
    name = next_label("CONST")
    @string_table[value] = name
    add_data("#{name}: db '#{value}', 0")
    return name
  end

  def make_float_constant(value)
    name = @float_table[value]
    if not name.nil?
      return name
    end
    name = next_label("FLOAT")
    @float_table[value] = name
    add_data("#{name}: dq #{value}")
    return name
  end

  def next_label(prefix)
    @id += 1
    return "#{prefix}_#{@id}"
  end

  def add_data(entry)
    if not @data.include?(entry)
      @data << entry
    end
  end

  def emit0(line)
    @code << line
  end

  def emit_label(label)
    emit0("#{label}:")
  end

  def emit(line)
    emit0("  #{line}")
  end

  def advance()
    @token = @lexer.next_token()
  end
end

