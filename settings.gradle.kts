rootProject.name = "Neki"

include(
    ":core",
    ":domain",
    ":application",
    ":bootstrap",
    ":modules:postgres",
    ":modules:redis",
    ":modules:s3",
    ":modules:kakao",
    ":modules:apple",
    ":modules:discord",
)
