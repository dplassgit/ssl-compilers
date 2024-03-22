package com.plasstech.lang.ssl;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class ParserTest {

  @Test
  public void empty() {
    ImmutableList<String> code = compile("");
    assertThat(code).hasSize(6);
  }

  @Test
  public void println() {
    ImmutableList<String> code = compile("println 3");
    assertThat(code).contains("  extern printf");
    assertThat(code).contains("  extern putchar");
  }

  @Test
  public void fact() {
    ImmutableList<String> code = compile(LexerTest.FACT);
    assertThat(code).contains("  extern printf");
    assertThat(code).contains("  extern putchar");
  }

  private ImmutableList<String> compile(String program) {
    Parser parser = new Parser(program);
    ImmutableList<String> code = parser.parse();
    System.err.println(Joiner.on("\n").join(code));
    return code;
  }
}
