package com.jamesward.springdevtoolsmcpserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.server.RouterFunctions;

import java.util.List;

/**
 * Auto-configuration for Spring DevTools MCP Server.
 * This will automatically register the MCP server in a Spring Boot application.
 */
@AutoConfiguration
public class SpringDevToolsMCPServerAutoConfiguration {

    @Bean
    public McpSyncServer devToolsMCPServer(ApplicationContext applicationContext, Environment environment) {
        DevToolsMCPServer devToolsMCPServer = new DevToolsMCPServer(applicationContext, environment);

        McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
            .tools(true)
            .logging()
            .build();

        ObjectMapper objectMapper = new ObjectMapper();

        WebFluxSseServerTransportProvider transport = new WebFluxSseServerTransportProvider(objectMapper, "/mcp");

        MethodToolCallbackProvider toolCallbackProvider = MethodToolCallbackProvider.builder()
            .toolObjects(devToolsMCPServer)
            .build();

        var tools = McpToolUtils.toSyncToolSpecification(List.of(toolCallbackProvider.getToolCallbacks()));

        // this sets up the transport with the handlers - yeah, fun side-effects
        McpSyncServer server = McpServer.sync(transport)
            .serverInfo("Spring Devtools MCP Server", "1.0.0")
            .capabilities(capabilities)
            .tools(tools)
            .build();

        int port = Integer.parseInt(environment.getProperty("devtools.mcp.port", "9999"));

        var httpHandler = RouterFunctions.toHttpHandler(transport.getRouterFunction());
        var webServer = new NettyReactiveWebServerFactory(port).getWebServer(httpHandler);
        webServer.start();

        return server;
    }
}
