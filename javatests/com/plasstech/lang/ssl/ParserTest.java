package com.plasstech.lang.ssl;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ParserTest {

  @Test
  public void empty() {
    Parser parser = new Parser("");
    ImmutableList<String> code = parser.parse();
    System.out.println(code);
    assertThat(code).isNotEmpty();
  }

  @Test
  public void println() {
    Parser parser = new Parser("println 3");
    ImmutableList<String> code = parser.parse();
    System.out.println(code);
    assertThat(code).isNotEmpty();
  }
}
