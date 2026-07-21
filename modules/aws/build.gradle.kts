val awsSdkVersion = "2.27.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    api("software.amazon.awssdk:aws-core:$awsSdkVersion")
    api("software.amazon.awssdk:s3:$awsSdkVersion")
}
