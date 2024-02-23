# SSL Overview

## Types

SSL supports the following types:
* integer (32 bits)
* floating point (64 bits)
* strings (null-terminated)

Currently, only comparisons (IF statements) use booleans.


## Variables

All variable names are one letter long.

Like in Fortran, variables `i` through `n` are integers.

Variables `a` through `h` are 64-bit floats.

(Future expansion: variables `o` through `r` are booleans.)

Variables `s` through `z` are strings.


## Constants

Integer constants can be from -2^31-1 to 2^31.

Floating point constants must include a dot (e.g., `3.0`)


## Comments

Use a hashtag (#) for a comment to end-of-line.


## Expressions

Integer or floating point math: `+ - * /`

Integer or floating point comparisons: `== != < <= > >=`

(Future expansion: Strings: `+ [index]` and comparisons.)


## Statements

There are assignments, control flow, and output statements.

### Assignments

`(variable) = (expression)`

Expressions are limited to a simple assignment or a single
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
