cc_library(
    name = "sslc_lib",
    srcs = [
        "lexer.cpp",
        "parser.cpp",
    ],
    hdrs = [
        "keyword.h",
        "lexer.h",
        "parser.h",
        "token.h",
    ],
)

cc_test(
    name = "tests",
    srcs = ["lexer_test.cpp"],
    deps = [
        ":sslc_lib",
        "@googletest//:gtest_main",
    ],
)

cc_binary(
    name = "sslc",
    srcs = ["sslc.cpp"],
    deps = ["sslc_lib"],
)
