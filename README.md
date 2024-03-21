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

I had heard lots about Kotlin but never written a line of code before this 
project. I think I enjoyed the Kotlin version the most so far, maybe because
of how it so easily builds string literals. The language reminds me of the best
parts of Java and the best parts of Python.

(At this point I started writing tests with the code, instead of having to run
against the samples manually.)

Ruby was ok. It thinks it's a better Python, but to me, I feel it's like
the worst of Python and JavaScript...

Scala was new for me as well. It seems to be a cross between Kotlin and Python.
I liked the ability to automatically run tests whenever a file changed (via the
`~test` command in `sbt`.)

I went back and added tests for Python, Go and C++, when I revamped the two-character
symbol lexical analysis.

The awk implementation was kind of fun, but annoying, because it's completely
un-type-checked. If you mistype a variable, it defaults to null/undefined, instead of
giving an error...

Future languages may include:
   * [d2lang](https://github.com/dplassgit/d2lang)
   * D
   * Rust
   * [Sly](https://github.com/dabeaz/sly) (a Python parser generator)
   * [ANTLR](https://www.antlr.org/) ("Another Tool for another parser generator")

