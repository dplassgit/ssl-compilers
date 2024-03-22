java_library(
    name = "ssl_tests",
    srcs = [
        "LexerTest.java",
        "ParserTest.java",
    ],
    deps = [
        "@sslcompilers_maven//:com_google_guava_guava",
        "@sslcompilers_maven//:com_google_truth_truth",
        "@sslcompilers_maven//:junit_junit",
        "//java/com/plasstech/lang/ssl:ssl",
    ],
)

java_test(
    name = "LexerTest",
    runtime_deps = [
        ":ssl_tests",
    ],
)

java_test(
    name = "ParserTest",
    runtime_deps = [
        ":ssl_tests",
    ],
)
