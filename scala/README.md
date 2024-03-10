# scala

## Running

From the `scala` directory: `sbt run`.

## Testing

From the `scala` directory: `sbt test`.


## Compiling a jar

Compile via:

`scalac src/main/scala/*.scala -d sslc.jar`

Then it can be run:

`scala sslc.jar`


## bazel

I haven't been able to find `rules_scala` for `MODULE.bazel` so I'm only
going to use `sbt run` for now.
