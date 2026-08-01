val awsSdkVersion = "2.27.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")

    // api: neki-application 의 media/infra/storage 어댑터가 S3Client·S3Presigner 를 직접 사용한다.
    //      implementation 으로 낮추면 소비자 컴파일이 깨진다 (51건).
    api("software.amazon.awssdk:s3:$awsSdkVersion")

    // s3 가 전이로 가져오므로 노출은 불필요하고, 버전 고정 목적으로만 선언한다.
    implementation("software.amazon.awssdk:aws-core:$awsSdkVersion")
}
