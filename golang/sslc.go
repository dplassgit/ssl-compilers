package main

import (
  "bufio"
  "fmt"
  "os"
)

func main() {
  scanner := bufio.NewScanner(os.Stdin)
  program := ""
  for scanner.Scan() {
    program += scanner.Text() + "\n"
  }

  parser := NewParser(program)
  for _, line := range parser.Parse() {
    fmt.Println(line)
  }
}
