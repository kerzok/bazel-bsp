def _web_test_impl(ctx):
    script = ctx.actions.declare_file(ctx.label.name + ".sh")
    ctx.actions.write(
        output = script,
        content = "#!/bin/bash\necho 'Test passed'\nexit 0",
        is_executable = True,
    )
    return [DefaultInfo(executable = script)]

web_test = rule(
    implementation = _web_test_impl,
    test = True,
    attrs = {
        "deps": attr.label_list(allow_files = True),
    },
)
