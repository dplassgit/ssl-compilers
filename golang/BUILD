load("@rules_go//go:def.bzl", "go_binary", "go_library")

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
    importpath = ".",
    visibility = ["//visibility:private"],
)
