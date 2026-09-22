rootProject.name = "Neki"

include(
    ":core",
    ":domain",
    ":apps:api",
    ":apps:batch",
    ":modules:postgres",
    ":modules:redis",
    ":modules:aws",
    ":modules:kakao",
    ":modules:discord",
    ":modules:jasypt",
    ":modules:apple",
    ":modules:firebase",
)
