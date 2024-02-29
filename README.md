# SSL Compilers

Compilers for "SSL", a "simple scripting language."

Just for fun, I want to write the same compiler in different source languages.

The compilers all emit x64 assembly language.

## Language

See [language overview](docs/LANGUAGE.md).

## Development history

I built the Python version first. It's also the smallest, at only about 500 lines.

Then I built the Java version, including some JUnit tests. I have the most
experience with Java (including the d2lang compiler).

C++ was a challenge, because I hadn't written any in a long time (like, 25 years).
I don't remember C++ being so annoying with declaring every method in the header
file.

Next I built the golang version. This is now the most golang I have ever written
(about 700 lines). Since golang doesn't really have classes, it was odd to have
to add `(this *Parser)` for every method. Also, golang doesn't have method 
overloading, which I also found surprising.

The C version was really annoying because:
  * It doesn't have "simple" string concatenation
  * No built-in map, set, list

I think I enjoyed the Kotlin version the most so far, if only because the way
it so easily builds string literals. The language reminds me of the best parts
of Java, and the best parts of Python.

Future languages may include:
   * Scala
   * d2lang
   * Ruby
   * D
   * Rust (although I had issues using bazel with Rust)
   * Sly (a Python parser generator)
   * ANTLR (another parser generator)
   * awk
   * bash script


