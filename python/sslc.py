import fileinput

from parser import Parser

def main():
  program = ''
  for line in fileinput.input():
      program += line
  p = Parser(program)
  p.parse()


if __name__ == '__main__':
  main()
