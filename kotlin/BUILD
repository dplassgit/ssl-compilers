load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "sslc_lib",
    srcs = [
        "lexer.kt",
        "parser.kt",
        "sslc.kt",
        "token.kt",
    ],
)

kt_jvm_library(
    name = "sslc_test_lib",
    srcs = [
        "lexer_test.kt",
    ],
    deps = [
        ":sslc_lib",
        "@sslcompilers_maven//:com_google_truth_truth",
        "@sslcompilers_maven//:junit_junit",
    ],
)

java_test(
    name = "lexer_test",
    test_class = "com.plasstech.lang.ssl.LexerTest",
    runtime_deps = [
        ":sslc_test_lib",
    ],
)

# This cannot run for the same reason as the kt_jvm_binary
#kt_jvm_test(
#  name="lexer_test",
#  srcs=[
#    "lexer_test.kt"],
#  deps=[":sslc_lib",
#    "@sslcompilers_maven//:com_google_guava_guava",
#    "@sslcompilers_maven//:com_google_truth_truth",
#    "@sslcompilers_maven//:junit_junit",
#  ]
#)

# I couldn't get kt_jvm_binary to work; it gives an error
# FATAL: ExecuteProgram(C:\users\dplas\_bazel_dplas\psv6umpe\execroot\_main\bazel-out\x64_windows-fastbuild\bin\kotlin\sslc_kt) failed: ERROR: src/main/native/windows/process.cc(202): CreateProcessW("C:\users\dplas\_bazel_dplas\psv6umpe\execroot\_main\bazel-out\x64_windows-fastbuild\bin\kotlin\sslc_kt"): %1 is not a valid Win32 application.
# (error: 193)

java_binary(
    name = "sslc",
    main_class = "com.plasstech.lang.ssl.Sslc",
    runtime_deps = [":sslc_lib"],
)
