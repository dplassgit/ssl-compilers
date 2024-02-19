load("@rules_go//go:def.bzl", "go_binary", "go_library")

go_binary(
    name = "sslc",
    embed = [":golang_lib"],  # why embed?
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
    importpath = ".", # golang/golang",  # why golang/golang?! makes no tidference
    visibility = ["//visibility:private"],
)
