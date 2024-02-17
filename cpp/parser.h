#ifndef __parser_h__
#define __parser_h__

#include <map>
#include <set>
#include <string>
#include <vector>

using namespace std;

class Parser {
public:
  Parser::Parser(Lexer &lexer) :
      lexer(lexer) {
    this->advance();
    arithOpcodes.insert( { PLUS, "add eax, ebx" });
    arithOpcodes.insert( { MULT, "imul eax, ebx" });
    arithOpcodes.insert( { DIV, "xchg eax, ebx\n  cdq\n  idiv ebx" });
    arithOpcodes.insert( { MINUS, "xchg eax, ebx\n  sub eax, ebx" });

    cmpOpcodes.insert( { EQEQ, "setz" });
    cmpOpcodes.insert( { NEQ, "setnz" });
    cmpOpcodes.insert( { LT, "setl" });
    cmpOpcodes.insert( { GT, "setg" });
    cmpOpcodes.insert( { GEQ, "setge" });
    cmpOpcodes.insert( { LEQ, "setle" });

  }

  vector<string> parse();

private:
  Lexer &lexer;
  Token *token = NULL;
  vector<string> code;
  set<string> data;
  int id = 0;

  // Keys are string values, values are names.
  map<string, string> stringTable;
  map<Symbol, string> arithOpcodes;
  map<Symbol, string> cmpOpcodes;

  void statements();
  void statement();
  void parsePrint();
  void parseIf();
  void parseFor();
  void assignment();
  VarType expr();
  VarType atom();

  string addStringConstant(string value);
  string nextLabel(string prefix);
  void expect(Symbol symbol);
  void expect(Keyword keyword);
  void checkTypes(VarType left, VarType right);

  void addData(string entry) {
    data.insert(entry);
  }

  void addData(string name, VarType varType) {
    switch (varType) {
      case INT:
        addData("_" + name + ": dd 0");
        break;
      case STR:
        addData("_" + name + ": dq 0");
        break;
      default:
        return;
    }
  }

  void advance() {
    token = lexer.nextToken();
  }

  void emit0(string line) {
    code.push_back(line);
  }

  void emit(string line) {
    emit0("  " + line);
  }
  void emitLabel(string label) {
    emit0(label + ":");
  }

  int nextInt() {
    return id++;
  }
};

#endif
