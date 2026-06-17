rootProject.name = "Neki"

include(
    ":core",
    ":domain",
    ":application",
    ":modules:postgres",
    ":modules:redis",
    ":modules:s3",
    ":modules:kakao",
    ":modules:discord",
)
