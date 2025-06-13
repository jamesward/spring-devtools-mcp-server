package com.jamesward.springdevtoolsmcpserver

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpSyncServer
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider
import io.modelcontextprotocol.spec.McpSchema
import org.springframework.ai.mcp.McpToolUtils
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.web.reactive.function.server.RouterFunctions

/**
 * Auto-configuration for Spring DevTools MCP Server.
 * This will automatically register the MCP server in a Spring Boot application.
 */
@AutoConfiguration
open class SpringDevToolsMCPServerAutoConfiguration {

    @Bean
    open fun devToolsMCPServer(applicationContext: ApplicationContext, environment: Environment): McpSyncServer? {
        val devToolsMCPServer = DevToolsMCPServer(applicationContext, environment)

        val capabilities = McpSchema.ServerCapabilities.builder()
            .tools(true)
            .logging()
            .build()

        val objectMapper = ObjectMapper()

        val transport = WebFluxSseServerTransportProvider(objectMapper, "/mcp")

        val toolCallbackProvider = MethodToolCallbackProvider.builder().toolObjects(devToolsMCPServer).build()

        val tools = McpToolUtils.toSyncToolSpecification(toolCallbackProvider.toolCallbacks.toList())

        // this sets up the transport with the handlers - yeah, fun side-effects
        val server = McpServer.sync(transport)
            .serverInfo("Spring Devtools MCP Server", "1.0.0")
            .capabilities(capabilities)
            .tools(tools)
            .build()

        val port = environment.getProperty("devtools.mcp.port", "9999").toInt()

        val httpHandler = RouterFunctions.toHttpHandler(transport.routerFunction)
        val webServer = NettyReactiveWebServerFactory(port).getWebServer(httpHandler)
        webServer.start()

        return server
    }
}
