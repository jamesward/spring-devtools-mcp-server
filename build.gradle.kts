plugins {
    `java-library`
    `maven-publish`
    signing
    id("org.danilopianini.publish-on-central") version "9.0.2"
}

group = "com.jamesward"
version = "0.0.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation("org.springframework.ai:spring-ai-mcp:1.0.0")
    implementation("io.modelcontextprotocol.sdk:mcp-spring-webflux:0.10.0")
    runtimeOnly("io.projectreactor.netty:reactor-netty-http:1.2.7")
//    api("io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.10.0")
    api("org.springframework.boot:spring-boot:3.5.3")
    api("org.springframework.boot:spring-boot-autoconfigure:3.5.3")
    compileOnly("org.springframework:spring-webmvc:6.2.1")
}

signing {
    sign(publishing.publications)
    useInMemoryPgpKeys(System.getenv("OSS_GPG_KEY"), System.getenv("OSS_GPG_PASS"))
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name = "Spring Devtools MCP Server"
                description = "An MCP Server that exposes the internals of a Spring application to AI code assistants"
                url = "https://github.com/jamesward/spring-devtools-mcp-server"

                scm {
                    connection = "scm:git:https://github.com/jamesward/spring-devtools-mcp-server.git"
                    developerConnection = "scm:git:git@github.com:jamesward/spring-devtools-mcp-server.git"
                    url = "https://github.com/jamesward/spring-devtools-mcp-server"
                }

                licenses {
                    license {
                        name = "Apache 2.0"
                        url = "https://opensource.org/licenses/Apache-2.0"
                    }
                }

                developers {
                    developer {
                        id = "jamesward"
                        name = "James Ward"
                        email = "james@jamesward.com"
                        url = "https://jamesward.com"
                    }
                }
            }
        }
    }
}
