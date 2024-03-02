require_relative "parser"

class Sslc
  def main
    program = ARGF.read 
    parser = Parser.new(program)
    code = parser.parse()
    puts code
  end
end

if __FILE__ == $0
  Sslc.new.main
end
