load("@rules_ruby//ruby:defs.bzl", "rb_binary", "rb_library", "rb_test")

rb_library(
    name = "sslc_lib",
    srcs = [
        "lexer.rb",
        "parser.rb",
        "sslc.rb",
        "token.rb",
    ],
)

rb_binary(
    name = "sslc",
    main = "sslc.rb",
    deps = [":sslc_lib"],
)

rb_test(
    name = "lexer_test",
    srcs = ["lexer_test.rb"],
    args = ["ruby/lexer_test.rb"],
    deps = [":sslc_lib"],
)

rb_test(
    name = "parser_test",
    srcs = ["parser_test.rb"],
    args = ["ruby/parser_test.rb"],
    deps = [":sslc_lib"],
)
