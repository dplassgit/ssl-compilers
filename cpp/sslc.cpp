#include "lexer.h"
#include "parser.h"
#include <string>
#include <vector>
#include <iostream>
using namespace std;

int main() {
  string text;
  while (!cin.eof()) {
    string line;
    getline(cin, line);
    text += line + "\n";
  }

  Lexer lexer(text);
  Parser parser(lexer);
  vector < string > code = parser.parse();
  for (vector<string>::iterator iter = code.begin(); iter < code.end();
      iter++) {
    cout << *iter << endl;
  }
}
