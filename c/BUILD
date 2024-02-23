cc_library(
    name = "sslc_lib",
    srcs = [
        "lexer.c",
        "token.c",
        "parser.c",
        "sslc.c",
    ],
    hdrs = [
        "lexer.h",
        "sslc.h",
        "parser.h",
        "token.h",
    ],
)

cc_binary(
    name = "sslc",
    deps = ["sslc_lib"],
)
