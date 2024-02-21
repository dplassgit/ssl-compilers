java_library(
    name = "ssl",
    srcs = [
        "ConstToken.java",
        "Keyword.java",
        "KeywordToken.java",
        "Lexer.java",
        "Parser.java",
        "Symbol.java",
        "SymbolToken.java",
        "Token.java",
        "TokenType.java",
        "TypedToken.java",
        "VarToken.java",
        "VarType.java",
    ],
    deps = ["@sslcompilers_maven//:com_google_guava_guava"],
)

java_binary(
    name = "sslc",
    srcs = ["Sslc.java"],
    main_class = "com.plasstech.lang.ssl.Sslc",
    deps = [
        ":ssl",
        "@sslcompilers_maven//:com_google_guava_guava",
    ],
)
