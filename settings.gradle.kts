rootProject.name = "Neki"

include(
    ":core",
    ":domain",
    ":apps:api",
    ":modules:postgres",
    ":modules:redis",
    ":modules:aws",
    ":modules:kakao",
    ":modules:discord",
    ":modules:jasypt",
    ":modules:apple",
    ":modules:firebase",
)
