# SSL Overview

Note: Not everything is implemented.


## Variables

All variables are one letter.

Like in Fortran, variables `i` through `n` are integers (32-bits).

(Future expansion: variables `a` through `h` are 64-bit floats.)

(Future expansion: variables `o` through `r` are booleans.)

Variables `s` through `z` are strings.


## Constants

Integer constants can be from -2^31-1 to 2^31.

Floating point constants must include a dot (e.g., `3.0`)


## Comments

Use a hashtag (#) for a comment to end-of-line.


## Expressions

Integer or floating point math: `+ - * /`

Comparisons: `== != < <= > >=`

(Future expansion: Strings: `+ [index]`)


## Statements

There are assignments, control flow, and output statements.

### Assignments

`(variable) = (expression)`

Expressions are limited to a simple assignment, a single unary or a single
binary expression, e.g.,

`a = 3.0`
`b = a + 1.0`
`c = a * b`

### Control flow

```
if [comparison expr] then
  statements
else
  statements
endif
```

```
for [int variable] = [int expr] to [int expr]
  statements
endfor
```

Future expansion: `for [int variable] = [int expr] to [int expr] step [int expr]`

### Output

`print expr`

`println expr`


## Example

Factorial:

```
j=1
n=10
for i = 1 to n
  j = j * i
endfor
println j
```
