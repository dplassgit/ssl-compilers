package com.plasstech.lang.ssl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

public class Sslc {
  public static void main(String args[]) {
    // read from stdin
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    String text = reader.lines().collect(Collectors.joining("\n"));

    // compile
    Parser parser = new Parser(text);
    ImmutableList<String> code = parser.parse();

    // write to stdout
    code.forEach(line -> System.out.println(line));
  }
}
