cc_library(
    name = "sslc_lib",
    srcs = [
        "lexer.cpp",
        "parser.cpp",
        "sslc.cpp",
    ],
    hdrs = [
        "lexer.h",
        "keyword.h",
        "parser.h",
        "token.h",
    ],
)

cc_binary(
    name = "sslc",
    deps = ["sslc_lib"],
)
