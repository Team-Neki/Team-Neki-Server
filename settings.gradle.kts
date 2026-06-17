rootProject.name = "Neki"

include(
    ":neki-core",
    ":neki-domain",
    ":neki-application",
    ":modules:neki-postgres",
    ":modules:neki-redis",
    ":modules:neki-s3",
    ":modules:neki-kakao",
    ":modules:neki-discord",
)
