package com.jamesward.springdevtoolsmcpserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configuration for Spring DevTools MCP Server.
 * This will automatically register the MCP server in a Spring Boot application.
 */
@AutoConfiguration
public class SpringDevToolsMCPServerAutoConfiguration {

    @Bean
    DevToolsMCPServer.StandardTools standardTools(ApplicationContext applicationContext, Environment environment) {
        return new DevToolsMCPServer.StandardTools(applicationContext, environment);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping")
    DevToolsMCPServer.WebMvcTools webMvcTools(ApplicationContext applicationContext) {
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        return new DevToolsMCPServer.WebMvcTools(mapping);
    }

    // todo: without shading the deps, this will always be there
    @Bean
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.server.RouterFunction")
    DevToolsMCPServer.WebFluxTools webFluxTools(ApplicationContext applicationContext) {
        return new DevToolsMCPServer.WebFluxTools(applicationContext);
    }

    @Bean
    McpSyncServer devToolsMCPServer(DevToolsMCPServer.StandardTools standardTools,
                                   List<DevToolsMCPServer.WebMvcTools> webMvcTools,
                                    List<DevToolsMCPServer.WebFluxTools> webFluxTools) {
        McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
            .tools(true)
            .logging()
            .build();

        ObjectMapper objectMapper = new ObjectMapper();

        WebFluxSseServerTransportProvider transport = new WebFluxSseServerTransportProvider(objectMapper, "/mcp");

        List<Object> toolObjects = new ArrayList<>();
        toolObjects.add(standardTools);
        toolObjects.addAll(webMvcTools);
        toolObjects.addAll(webFluxTools);

        MethodToolCallbackProvider toolCallbackProvider = MethodToolCallbackProvider.builder()
            .toolObjects(toolObjects.toArray())
            .build();

        var tools = McpToolUtils.toSyncToolSpecification(List.of(toolCallbackProvider.getToolCallbacks()));

        // this sets up the transport with the handlers - yeah, fun side-effects
        McpSyncServer server = McpServer.sync(transport)
            .serverInfo("Spring Devtools MCP Server", "1.0.0")
            .capabilities(capabilities)
            .tools(tools)
            .build();


//        int port = Integer.parseInt(environment.getProperty("devtools.mcp.port", "9999"));

        // todo: externalize port
        var httpHandler = RouterFunctions.toHttpHandler(transport.getRouterFunction());
        var webServer = new NettyReactiveWebServerFactory(9999).getWebServer(httpHandler);
        webServer.start();

        return server;
    }
}
