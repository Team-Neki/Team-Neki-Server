rootProject.name = "Neki"

include(
    ":neki-core",
    ":neki-domain",
    ":neki-application",
    ":modules:postgres",
    ":modules:redis",
    ":modules:s3",
    ":modules:kakao",
    ":modules:discord",
    ":modules:jasypt",
    ":modules:apple",
)
