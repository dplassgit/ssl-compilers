py_library(
    name = "sslc_lib",
    srcs = [
        "lexer.py",
        "parser.py",
        "ssl_token.py",
    ],
)

py_binary(
    name = "sslc",
    srcs = [
      "lexer.py",
      "parser.py",
      "sslc.py",
      "ssl_token.py",
    ],
    imports=['.'],
)

py_test(
    name="lexer_test",
    srcs=["lexer_test.py"],
    imports=['.'],
    deps=[":sslc_lib"]
)
