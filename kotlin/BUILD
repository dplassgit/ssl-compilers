load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")

kt_jvm_library(
    name = "sslc_lib",
    srcs = [
        "lexer.kt",
        "parser.kt",
        "sslc.kt",
        "token.kt",
    ],
)

# I couldn't get kt_jvm_binary to work; it gies an error
# FATAL: ExecuteProgram(C:\users\dplas\_bazel_dplas\psv6umpe\execroot\_main\bazel-out\x64_windows-fastbuild\bin\kotlin\sslc_kt) failed: ERROR: src/main/native/windows/process.cc(202): CreateProcessW("C:\users\dplas\_bazel_dplas\psv6umpe\execroot\_main\bazel-out\x64_windows-fastbuild\bin\kotlin\sslc_kt"): %1 is not a valid Win32 application.
# (error: 193)

java_binary(
    name = "sslc",
    main_class = "com.plasstech.lang.ssl.Sslc",
    runtime_deps = [":sslc_lib"],
)
