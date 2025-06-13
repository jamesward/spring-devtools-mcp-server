plugins {
    `java-library`
    id("org.jreleaser") version "1.17.0"
}

group = "com.jamesward"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    api("org.springframework.ai:spring-ai-mcp:1.0.0")
    api("io.modelcontextprotocol.sdk:mcp-spring-webflux:0.10.0")
    api("org.springframework.boot:spring-boot:3.5.0")
    api("org.springframework.boot:spring-boot-autoconfigure:3.5.0")
}

jreleaser {
    project {
        // todo: why not authors = ["asdf"]
        authors.add("James Ward")
        license = "Apache-2.0"
        inceptionYear = "2025"
    }
    release {
        github {
            repoOwner = "jamesward"
        }
    }
}
