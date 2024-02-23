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
    arithOpcodes.insert( { {PLUS, INT}, "add EAX, EBX" });
    arithOpcodes.insert( { {MULT, INT}, "imul EAX, EBX" });
    arithOpcodes.insert( { {DIV, INT}, "xchg EAX, EBX\n  cdq\n  idiv EBX" });
    arithOpcodes.insert( { {MINUS, INT}, "xchg EAX, EBX\n  sub EAX, EBX" });

    arithOpcodes.insert( { {PLUS, FLOAT},  "addsd XMM0, XMM1" });
    arithOpcodes.insert( { {MULT, FLOAT}, "mulsd XMM0, XMM1" });
    arithOpcodes.insert( { {DIV, FLOAT}, "divsd XMM1, XMM0\n  movsd XMM0, XMM1" });
    arithOpcodes.insert( { {MINUS, FLOAT}, "subsd XMM1, XMM0\n  movsd XMM0, XMM1" });

    cmpOpcodes.insert( { { EQEQ, INT}, "cmp EBX, EAX\n  setz AL" });
    cmpOpcodes.insert( { { NEQ, INT}, "cmp EBX, EAX\n  setnz AL" });
    cmpOpcodes.insert( { { LT, INT}, "cmp EBX, EAX\n  setl AL" });
    cmpOpcodes.insert( { { GT, INT}, "cmp EBX, EAX\n  setg AL" });
    cmpOpcodes.insert( { { GEQ, INT}, "cmp EBX, EAX\n  setge AL"});
    cmpOpcodes.insert( { { LEQ, INT}, "cmp EBX, EAX\n  setle AL" });

    cmpOpcodes.insert( { { EQEQ, FLOAT}, "comisd XMM1, XMM0\n  setz AL" });
    cmpOpcodes.insert( { { NEQ, FLOAT}, "comisd XMM1, XMM0\n  setnz AL" });
    cmpOpcodes.insert( { { LT, FLOAT}, "comisd XMM1, XMM0\n  setb AL" });
    cmpOpcodes.insert( { { GT, FLOAT}, "comisd XMM1, XMM0\n  seta AL" });
    cmpOpcodes.insert( { { GEQ, FLOAT}, "comisd XMM1, XMM0\n  setbe AL"});
    cmpOpcodes.insert( { { LEQ, FLOAT}, "comisd XMM1, XMM0\n  setae AL" });
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
  map<string, string> floatTable;
  map<pair<Symbol, VarType>, string> arithOpcodes;
  map<pair<Symbol, VarType>, string> cmpOpcodes;

  void statements();
  void statement();
  void parsePrint();
  void parseIf();
  void parseFor();
  void assignment();
  VarType expr();
  VarType atom();

  string addStringConstant(string value);
  string addFloatConstant(string value);
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
      case FLOAT:
        addData("_" + name + ": dq 0.0");
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
