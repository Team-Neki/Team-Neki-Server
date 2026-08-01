val jasyptVersion = "3.0.5"

dependencies {
    // api: apps/api 의 JasyptTest 가 StringEncryptor 를 직접 사용한다 (테스트 전용).
    //      implementation 으로 낮추면 소비자 테스트 컴파일이 깨진다 (17건).
    api("com.github.ulisesbocchio:jasypt-spring-boot-starter:$jasyptVersion")
    implementation("org.springframework:spring-context")
}
