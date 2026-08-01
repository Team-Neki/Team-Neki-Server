val firebaseAdminVersion = "9.9.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")

    // api: neki-application 의 notification/infra/fcm 어댑터가 FirebaseMessaging 을 직접 사용한다.
    //      implementation 으로 낮추면 소비자 컴파일이 깨진다 (28건).
    api("com.google.firebase:firebase-admin:$firebaseAdminVersion")
}
