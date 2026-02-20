load(":web_test_rules.bzl", "web_test")

web_test(
    name = "app_test",
    deps = ["app.ts"],
)

# This target should be excluded by the lint filter in extract_typescript_info.
web_test(
    name = "lint",
    deps = ["app.ts"],
)
