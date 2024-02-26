package com.plasstech.lang.ssl
import kotlin.io.readLine

class Sslc {
  companion object {
    @JvmStatic
    private fun tokens(program: String) {
      //val program = "pRiNtLn A J S 3" //  34.56 \n #comment \n 0.14"
      println("Program:\n$program")
      val lexer = Lexer(program)
      var token = lexer.nextToken()
      println(token)
      while (token.type != TokenType.EndOfFile) {
        token = lexer.nextToken()
        println(token)
      }
    }

    @JvmStatic
    private fun compile(program: String) {
      // Regular compile
      val parser = Parser(program)
      parser.parse()
    }
    
    @JvmStatic
    fun main(args: Array<String>) {
      var program = ""
      var line = readLine()
      while (line != null) {
        program += line + "\n"
        line = readLine()
      }

      if (args.size > 0 && args[0] == "--tokensonly") {
        tokens(program)
        return
      }
      compile(program)
    }
  }
}
