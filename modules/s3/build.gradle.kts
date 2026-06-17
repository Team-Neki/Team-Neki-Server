val awsSdkVersion = "2.27.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("software.amazon.awssdk:aws-core:$awsSdkVersion")
    implementation("software.amazon.awssdk:s3:$awsSdkVersion")
}
