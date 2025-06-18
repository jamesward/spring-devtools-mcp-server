plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.spring") version "2.1.21"
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.jamesward"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    developmentOnly("com.jamesward:spring-devtools-mcp-server")
//    developmentOnly("com.jamesward:spring-devtools-mcp-server:0.0.1")
}
