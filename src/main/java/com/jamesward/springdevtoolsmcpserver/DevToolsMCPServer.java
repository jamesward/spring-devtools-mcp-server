package com.jamesward.springdevtoolsmcpserver;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * MCP Server for Spring developers that provides tools to query information about a running Spring application.
 */
public class DevToolsMCPServer {

    DevToolsMCPServer() {

    }

    static class StandardTools {

        private final ApplicationContext applicationContext;
        private final Environment environment;

        protected StandardTools(ApplicationContext applicationContext, Environment environment) {
            this.applicationContext = applicationContext;
            this.environment = environment;
        }

        @Tool(description = "Gets all Spring beans registered in the application context")
        public Map<String, String> getSpringBeans() {
            Map<String, String> result = new HashMap<>();
            for (String beanName : applicationContext.getBeanDefinitionNames()) {
                try {
                    Object bean = applicationContext.getBean(beanName);
                    result.put(beanName, bean.getClass().getName());
                } catch (Exception e) {
                    result.put(beanName, "Error loading bean: " + e.getMessage());
                }
            }
            return result;
        }

        @Tool(description = "Gets details about a specific Spring bean by name")
        public Map<String, Object> getBeanDetails(@ToolParam(description = "name of the bean") String beanName) {
            if (!applicationContext.containsBean(beanName)) {
                return Collections.singletonMap("error", "Bean not found: " + beanName);
            }

            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = bean.getClass();

            Map<String, Object> details = new HashMap<>();
            details.put("name", beanName);
            details.put("class", beanClass.getName());
            details.put("type", beanClass.getSimpleName());

            List<String> interfaces = new ArrayList<>();
            for (Class<?> iface : beanClass.getInterfaces()) {
                interfaces.add(iface.getName());
            }
            details.put("interfaces", interfaces);

            details.put("singleton", applicationContext.isSingleton(beanName));
            details.put("prototype", applicationContext.isPrototype(beanName));

            return details;
        }

        @Tool(description = "Gets all active Spring profiles")
        public List<String> getActiveProfiles() {
            return Arrays.asList(environment.getActiveProfiles());
        }

        @Tool(description = "Gets health information about the application")
        public Map<String, Object> getHealthInfo() {
            Map<String, Object> healthInfo = new HashMap<>();

            // Basic JVM info
            Runtime runtime = Runtime.getRuntime();
            Map<String, String> memoryInfo = new HashMap<>();
            memoryInfo.put("maxMemory", runtime.maxMemory() / (1024 * 1024) + " MB");
            memoryInfo.put("totalMemory", runtime.totalMemory() / (1024 * 1024) + " MB");
            memoryInfo.put("freeMemory", runtime.freeMemory() / (1024 * 1024) + " MB");
            memoryInfo.put("usedMemory", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + " MB");

            healthInfo.put("memory", memoryInfo);
            healthInfo.put("processors", runtime.availableProcessors());
            healthInfo.put("javaVersion", System.getProperty("java.version"));
            healthInfo.put("javaVendor", System.getProperty("java.vendor"));
            healthInfo.put("osName", System.getProperty("os.name"));
            healthInfo.put("osVersion", System.getProperty("os.version"));
            healthInfo.put("startTime", new Date(ManagementFactory.getRuntimeMXBean().getStartTime()).toString());
            healthInfo.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000 + " seconds");

            // Thread information
            Map<String, Object> threadInfo = new HashMap<>();
            threadInfo.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
            threadInfo.put("peakThreadCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
            threadInfo.put("daemonThreadCount", ManagementFactory.getThreadMXBean().getDaemonThreadCount());
            threadInfo.put("totalStartedThreadCount", ManagementFactory.getThreadMXBean().getTotalStartedThreadCount());
            healthInfo.put("threads", threadInfo);

            return healthInfo;
        }

        @Tool(description = "Gets information about application dependencies")
        public List<Map<String, String>> getDependencyInfo() {
            List<Map<String, String>> dependencies = new ArrayList<>();

            try {
                Package[] packages = Package.getPackages();
                for (Package pkg : packages) {
                    String specTitle = pkg.getSpecificationTitle();
                    String specVersion = pkg.getSpecificationVersion();
                    String implTitle = pkg.getImplementationTitle();
                    String implVersion = pkg.getImplementationVersion();

                    if ((specTitle != null && specVersion != null) || (implTitle != null && implVersion != null)) {
                        Map<String, String> dependency = new HashMap<>();
                        dependency.put("name", (implTitle != null ? implTitle : (specTitle != null ? specTitle : pkg.getName())));
                        dependency.put("version", (implVersion != null ? implVersion : (specVersion != null ? specVersion : "unknown")));
                        dependency.put("package", pkg.getName());
                        dependencies.add(dependency);
                    }
                }
            } catch (Exception e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Failed to get dependency info: " + e.getMessage());
                dependencies.add(error);
            }

            return dependencies;
        }

        @Tool(description = "Gets a specific Spring property value by key")
        public String getSpringProperty(@ToolParam(description = "property key to retrieve") String key) {
            return environment.getProperty(key);
        }

        @Tool(description = "Gets all Spring properties with optional prefix filter")
        public Map<String, String> getSpringProperties(@ToolParam(description = "optional prefix to filter properties (e.g., 'server.', 'spring.')", required = false) String prefix) {
            Map<String, String> properties = new HashMap<>();

            if (environment instanceof org.springframework.core.env.AbstractEnvironment) {
                org.springframework.core.env.AbstractEnvironment abstractEnv = (org.springframework.core.env.AbstractEnvironment) environment;
                for (org.springframework.core.env.PropertySource<?> propertySource : abstractEnv.getPropertySources()) {
                    if (propertySource instanceof org.springframework.core.env.EnumerablePropertySource) {
                        org.springframework.core.env.EnumerablePropertySource<?> enumerable = (org.springframework.core.env.EnumerablePropertySource<?>) propertySource;
                        for (String key : enumerable.getPropertyNames()) {
                            if (prefix == null || key.startsWith(prefix)) {
                                String value = environment.getProperty(key);
                                if (value != null) {
                                    properties.put(key, value);
                                }
                            }
                        }
                    }
                }
            }

            return properties;
        }

        @Tool(description = "Gets information about auto-configuration in the Spring application")
        public Map<String, Object> getAutoConfigurationInfo() {
            Map<String, Object> autoConfigInfo = new HashMap<>();

            try {
                // Try to get the auto-configuration report bean if available (Spring Boot 2.x)
                try {
                    Object autoConfigurationReport = applicationContext.getBean("autoConfigurationReport");
                    if (autoConfigurationReport != null) {
                        // Use reflection to access the report data
                        Class<?> reportClass = autoConfigurationReport.getClass();
                        Object positiveMatches = reportClass.getMethod("getPositiveMatches").invoke(autoConfigurationReport);
                        Object negativeMatches = reportClass.getMethod("getNegativeMatches").invoke(autoConfigurationReport);

                        autoConfigInfo.put("positiveMatches", positiveMatches);
                        autoConfigInfo.put("negativeMatches", negativeMatches);
                    }
                } catch (Exception e) {
                    // Auto-configuration report not available in this format
                }

                // For Spring Boot 3.x, try to get the ConditionEvaluationReport
                try {
                    Class<?> reportClass = Class.forName("org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport");
                    Object reportInstance = applicationContext.getBean(reportClass);

                    if (reportInstance != null) {
                        // Get unconditional classes
                        java.lang.reflect.Method getUnconditionalClassesMethod = reportClass.getMethod("getUnconditionalClasses");
                        Set<?> unconditionalClasses = (Set<?>) getUnconditionalClassesMethod.invoke(reportInstance);
                        autoConfigInfo.put("unconditionalClasses", unconditionalClasses);

                        // Get condition evaluations
                        java.lang.reflect.Method getConditionAndOutcomesBySourceMethod =
                                reportClass.getMethod("getConditionAndOutcomesBySource");
                        Map<?, ?> conditionAndOutcomesBySource =
                                (Map<?, ?>) getConditionAndOutcomesBySourceMethod.invoke(reportInstance);

                        Map<String, Object> conditionEvaluations = new HashMap<>();
                        for (Map.Entry<?, ?> entry : conditionAndOutcomesBySource.entrySet()) {
                            String source = entry.getKey().toString();
                            Object outcomes = entry.getValue();

                            // Extract outcome information
                            List<Map<String, String>> outcomeDetails = new ArrayList<>();
                            if (outcomes instanceof Collection<?>) {
                                for (Object outcome : (Collection<?>) outcomes) {
                                    Map<String, String> outcomeInfo = new HashMap<>();
                                    // Get condition information
                                    java.lang.reflect.Method getConditionMethod = outcome.getClass().getMethod("getCondition");
                                    Object condition = getConditionMethod.invoke(outcome);
                                    outcomeInfo.put("condition", condition.toString());

                                    // Get outcome information
                                    java.lang.reflect.Method getOutcomeMethod = outcome.getClass().getMethod("getOutcome");
                                    Object outcomeResult = getOutcomeMethod.invoke(outcome);
                                    outcomeInfo.put("outcome", outcomeResult.toString());

                                    outcomeDetails.add(outcomeInfo);
                                }
                            }
                            conditionEvaluations.put(source, outcomeDetails);
                        }
                        autoConfigInfo.put("conditionEvaluations", conditionEvaluations);

                        // Get exclusions
                        java.lang.reflect.Method getExclusionsMethod = reportClass.getMethod("getExclusions");
                        Set<?> exclusions = (Set<?>) getExclusionsMethod.invoke(reportInstance);
                        autoConfigInfo.put("exclusions", exclusions);
                    }
                } catch (Exception e) {
                    // ConditionEvaluationReport not available or reflection failed
                    autoConfigInfo.put("error", "Could not access ConditionEvaluationReport: " + e.getMessage());
                }

                // Get all beans with @Configuration annotation
                Map<String, Object> configurationBeans = new HashMap<>();
                for (String beanName : applicationContext.getBeanDefinitionNames()) {
                    try {
                        Object bean = applicationContext.getBean(beanName);
                        Class<?> beanClass = bean.getClass();

                        // Check if the bean class has @Configuration annotation
                        if (beanClass.isAnnotationPresent(org.springframework.context.annotation.Configuration.class)) {
                            Map<String, Object> configInfo = new HashMap<>();
                            configInfo.put("class", beanClass.getName());

                            // Check for @ConditionalOn* annotations
                            List<String> conditions = new ArrayList<>();
                            for (java.lang.annotation.Annotation annotation : beanClass.getAnnotations()) {
                                String annotationName = annotation.annotationType().getName();
                                if (annotationName.contains("Conditional")) {
                                    conditions.add(annotationName);
                                }
                            }
                            configInfo.put("conditions", conditions);

                            // Check if it's an auto-configuration
                            boolean isAutoConfig = beanClass.isAnnotationPresent(org.springframework.boot.autoconfigure.AutoConfiguration.class) ||
                                    beanClass.isAnnotationPresent(org.springframework.boot.autoconfigure.EnableAutoConfiguration.class);
                            configInfo.put("isAutoConfiguration", isAutoConfig);

                            configurationBeans.put(beanName, configInfo);
                        }
                    } catch (Exception e) {
                        // Skip beans that can't be analyzed
                    }
                }
                autoConfigInfo.put("configurationBeans", configurationBeans);

            } catch (Exception e) {
                autoConfigInfo.put("error", "Failed to get auto-configuration info: " + e.getMessage());
            }

            return autoConfigInfo;
        }
    }

    static class WebMvcTools {
        private final ApplicationContext applicationContext;

        protected WebMvcTools(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Tool(description = "Gets all HTTP routes registered in the Spring application")
        public List<Map<String, Object>> getHttpRoutes() {
            List<Map<String, Object>> routes = new ArrayList<>();

            var mappings = applicationContext.getBeansOfType(RequestMappingHandlerMapping.class);

            mappings.values().forEach(requestMappingHandlerMapping -> {
                try {
                    Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

                    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
                        RequestMappingInfo mappingInfo = entry.getKey();
                        HandlerMethod handlerMethod = entry.getValue();

                        Map<String, Object> route = new HashMap<>();
                        route.put("mappingInfo", mappingInfo);
                        route.put("controller", handlerMethod.getBeanType().getSimpleName());
                        route.put("fullController", handlerMethod.getBeanType().getName());
                        route.put("method", handlerMethod.getMethod().toGenericString());

                        routes.add(route);
                    }
                } catch (Exception e) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Failed to get HTTP routes: " + e.getMessage());
                    routes.add(error);
                }
            });

            return routes;
        }
    }

    static class WebFluxTools {
        private final ApplicationContext applicationContext;

        protected WebFluxTools(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Tool(description = "Gets all WebFlux routes registered in the Spring application")
        public List<Map<String, Object>> getWebFluxRoutes() {
            List<Map<String, Object>> routes = new ArrayList<>();

            try {
                // Get the RouterFunction beans
                Map<String, org.springframework.web.reactive.function.server.RouterFunction> routerFunctions =
                        applicationContext.getBeansOfType(org.springframework.web.reactive.function.server.RouterFunction.class);

                // Get the RouterFunctionMapping if available
                Object routerFunctionMapping = null;
                try {
                    routerFunctionMapping = applicationContext.getBean("routerFunctionMapping");
                } catch (Exception e) {
                    // RouterFunctionMapping not available
                }

                // Add information about router functions
                for (Map.Entry<String, org.springframework.web.reactive.function.server.RouterFunction> entry : routerFunctions.entrySet()) {
                    Map<String, Object> route = new HashMap<>();
                    route.put("beanName", entry.getKey());
                    route.put("type", "RouterFunction");
                    route.put("class", entry.getValue().getClass().getName());
                    routes.add(route);
                }

                // Get the RequestMappingHandlerMapping for WebFlux if available
                Object handlerMapping = null;
                try {
                    handlerMapping = applicationContext.getBean("requestMappingHandlerMapping");
                    if (handlerMapping.getClass().getName().contains("reactive")) {
                        // This is likely the WebFlux RequestMappingHandlerMapping
                        // Use reflection to get the handler methods
                        java.lang.reflect.Method getHandlerMethodsMethod =
                                handlerMapping.getClass().getMethod("getHandlerMethods");
                        Map<?, ?> handlerMethods = (Map<?, ?>) getHandlerMethodsMethod.invoke(handlerMapping);

                        for (Map.Entry<?, ?> handlerEntry : handlerMethods.entrySet()) {
                            Map<String, Object> route = new HashMap<>();
                            route.put("type", "AnnotatedController");
                            route.put("mapping", handlerEntry.getKey().toString());
                            route.put("handler", handlerEntry.getValue().toString());
                            routes.add(route);
                        }
                    }
                } catch (Exception e) {
                    // RequestMappingHandlerMapping not available or reflection failed
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Could not access WebFlux handler mappings: " + e.getMessage());
                    routes.add(error);
                }
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to get WebFlux routes: " + e.getMessage());
                routes.add(error);
            }

            return routes;
        }
    }

}
