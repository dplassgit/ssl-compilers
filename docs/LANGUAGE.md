# SSL Overview

## Variables

All variables are one letter.

Like in Fortran, variables `i` through `n` are integers (64-bits). 

Variables `a` through `h` are floats (64-bits)

(Future expansion: variables `o` through `r` are booleans)

Variables `s` through `z` are strings.

## Expressions

Integer or floating point math: `+ - * / %`

Comparisons: `== != < <= > >=`

Strings: `+ [index]`


## Control flow

`if [comparison] then ... else ... endif`

`for [int variable] = expr to expr [step expr] ... endfor`


## Output

`print expr`

`println expr`



## Example

Factorial:

```
f=1.0
g=1.0
for i = 1 to 10
  f = f * g
  g = g + 1.0
endfor
println f
```
