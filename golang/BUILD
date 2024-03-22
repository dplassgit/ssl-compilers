load("@rules_go//go:def.bzl", "go_binary", "go_library", "go_test")

go_binary(
    name = "sslc",
    embed = [":golang_lib"],
    visibility = ["//visibility:public"],
)

go_library(
    name = "golang_lib",
    srcs = [
        "lexer.go",
        "parser.go",
        "sslc.go",
        "token.go",
    ],
    visibility = ["//visibility:private"],
)

go_test(
    name = "tests",
    srcs = [
        "lexer_test.go",
    ],
    # This cannot be deps
    embed = [":golang_lib"],
    # This is relative to the workspace root.
    importpath = "golang",
    visibility = ["//visibility:private"],
)
