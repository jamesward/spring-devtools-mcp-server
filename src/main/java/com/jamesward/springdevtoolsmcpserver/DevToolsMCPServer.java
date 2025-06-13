package com.jamesward.springdevtoolsmcpserver;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * MCP Server for Spring developers that provides tools to query information about a running Spring application.
 */
public class DevToolsMCPServer {
    
    private final ApplicationContext applicationContext;
    private final Environment environment;

    public DevToolsMCPServer(ApplicationContext applicationContext, Environment environment) {
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
}