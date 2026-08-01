dependencies {
    // api: neki-application 의 */infra/cache, media/infra/lock 어댑터가 RedisTemplate 을 직접 사용한다.
    //      implementation 으로 낮추면 소비자 컴파일이 깨진다 (25건).
    api("org.springframework.boot:spring-boot-starter-data-redis")
}
