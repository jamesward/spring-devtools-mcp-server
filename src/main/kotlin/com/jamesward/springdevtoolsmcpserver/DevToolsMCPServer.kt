package com.jamesward.springdevtoolsmcpserver

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
//import org.springframework.web.method.HandlerMethod
//import org.springframework.web.servlet.mvc.method.RequestMappingInfo
//import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.lang.management.ManagementFactory
import java.util.*

/**
 * MCP Server for Spring developers that provides tools to query information about a running Spring application.
 */
class DevToolsMCPServer(val applicationContext: ApplicationContext, val environment: Environment) { // , val conditionEvaluationReport: ConditionEvaluationReport

    @Tool(description = "Gets all Spring beans registered in the application context")
    fun getSpringBeans(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        applicationContext.beanDefinitionNames.forEach { beanName ->
            try {
                val bean = applicationContext.getBean(beanName)
                result[beanName] = bean.javaClass.name
            } catch (e: Exception) {
                result[beanName] = "Error loading bean: ${e.message}"
            }
        }
        return result
    }

    @Tool(description = "Gets details about a specific Spring bean by name")
    fun getBeanDetails(@ToolParam(description = "name of the bean") beanName: String): Map<String, Any?> {
        if (!applicationContext.containsBean(beanName)) {
            return mapOf("error" to "Bean not found: $beanName")
        }

        val bean = applicationContext.getBean(beanName)
        val beanClass = bean.javaClass

        return mapOf(
            "name" to beanName,
            "class" to beanClass.name,
//            "scope" to applicationContext.getBeanFactory().getBeanDefinition(beanName).scope,
            "type" to beanClass.simpleName,
            "interfaces" to beanClass.interfaces.map { it.name },
            "singleton" to applicationContext.isSingleton(beanName),
            "prototype" to applicationContext.isPrototype(beanName)
        )
    }

    @Tool(description = "Gets all active Spring profiles")
    fun getActiveProfiles(): List<String> {
        return environment.activeProfiles.toList()
    }

    /*
    @Tool(description = "Gets properties from the Spring environment, optionally filtered by prefix")
    fun getProperties(prefix: String? = null): Map<String, String> {
        val result = mutableMapOf<String, String>()

        val propertyNames = environment.propertySources
            .flatMap { propertySource ->
                if (propertySource.source is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    (propertySource.source as Map<String, Any>).keys
                } else {
                    emptyList()
                }
            }
            .filter { prefix == null || it.startsWith(prefix) }

        propertyNames.forEach { name ->
            val value = environment.getProperty(name)
            if (value != null) {
                // Mask sensitive properties
                if (name.contains("password", ignoreCase = true) ||
                    name.contains("secret", ignoreCase = true) ||
                    name.contains("key", ignoreCase = true)) {
                    result[name] = "******"
                } else {
                    result[name] = value
                }
            }
        }

        return result
    }
     */

    /*
    @Tool(description = "Gets all REST endpoints in the application")
    fun getRestEndpoints(): List<Map<String, Any>> {
        val endpoints = mutableListOf<Map<String, Any>>()

        requestMappingHandlerMapping?.let { mapping ->
            val handlerMethods = mapping.handlerMethods

            for ((requestMappingInfo, handlerMethod) in handlerMethods) {
                val patterns = requestMappingInfo?.patternsCondition?.patterns ?: emptySet()
                val methods = requestMappingInfo?.methodsCondition?.methods?.map { it.name } ?: listOf("ALL")
                val consumes = requestMappingInfo?.consumesCondition?.consumableMediaTypes?.map { it.toString() } ?: emptyList()
                val produces = requestMappingInfo?.producesCondition?.producibleMediaTypes?.map { it.toString() } ?: emptyList()

                patterns.forEach { pattern ->
                    endpoints.add(mapOf(
                        "path" to pattern,
                        "methods" to methods,
                        "consumes" to consumes,
                        "produces" to produces,
                        "controller" to handlerMethod.beanType.simpleName,
                        "method" to handlerMethod.method.name,
                        "parameters" to handlerMethod.method.parameters.map {
                            mapOf(
                                "name" to it.name,
                                "type" to it.type.simpleName
                            )
                        }
                    ))
                }
            }
        }

        return endpoints
    }
     */

    /*
    @Tool(description = "Gets information about auto-configuration in the Spring application")
    fun getAutoConfigurationReport(): Map<String, Any> {
        val report = mutableMapOf<String, Any>()

        conditionEvaluationReport?.let { evalReport ->
            report["positiveMatches"] = evalReport.conditionAndOutcomesBySource
                .filter { it.value.isFullMatch }
                .map { it.key }

            report["negativeMatches"] = evalReport.conditionAndOutcomesBySource
                .filter { !it.value.isFullMatch }
                .map { entry ->
                    mapOf(
                        "configuration" to entry.key,
                        "outcomes" to entry.value.outcomes.map {
                            mapOf(
                                "condition" to it.condition.toString(),
                                "outcome" to it.outcome
                            )
                        }
                    )
                }
        } ?: run {
            report["error"] = "ConditionEvaluationReport not available"
        }

        return report
    }
     */

    @Tool(description = "Gets health information about the application")
    fun getHealthInfo(): Map<String, Any> {
        val healthInfo = mutableMapOf<String, Any>()

        // Basic JVM info
        val runtime = Runtime.getRuntime()
        val memoryInfo = mapOf(
            "maxMemory" to "${runtime.maxMemory() / (1024 * 1024)} MB",
            "totalMemory" to "${runtime.totalMemory() / (1024 * 1024)} MB",
            "freeMemory" to "${runtime.freeMemory() / (1024 * 1024)} MB",
            "usedMemory" to "${(runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)} MB"
        )

        healthInfo["memory"] = memoryInfo
        healthInfo["processors"] = runtime.availableProcessors()
        healthInfo["javaVersion"] = System.getProperty("java.version")
        healthInfo["javaVendor"] = System.getProperty("java.vendor")
        healthInfo["osName"] = System.getProperty("os.name")
        healthInfo["osVersion"] = System.getProperty("os.version")
        healthInfo["startTime"] = Date(ManagementFactory.getRuntimeMXBean().startTime).toString()
        healthInfo["uptime"] = "${ManagementFactory.getRuntimeMXBean().uptime / 1000} seconds"

        // Thread information
        val threadMXBean = ManagementFactory.getThreadMXBean()
        val threadInfo = mapOf(
            "threadCount" to threadMXBean.threadCount,
            "peakThreadCount" to threadMXBean.peakThreadCount,
            "daemonThreadCount" to threadMXBean.daemonThreadCount,
            "totalStartedThreadCount" to threadMXBean.totalStartedThreadCount
        )
        healthInfo["threads"] = threadInfo

        return healthInfo
    }

    @Tool(description = "Gets information about application dependencies")
    fun getDependencyInfo(): List<Map<String, String>> {
        val dependencies = mutableListOf<Map<String, String>>()

        try {
            val packages = Package.getPackages()
            packages.forEach { pkg ->
                val specTitle = pkg.specificationTitle
                val specVersion = pkg.specificationVersion
                val implTitle = pkg.implementationTitle
                val implVersion = pkg.implementationVersion

                if ((specTitle != null && specVersion != null) || (implTitle != null && implVersion != null)) {
                    dependencies.add(mapOf(
                        "name" to (implTitle ?: specTitle ?: pkg.name),
                        "version" to (implVersion ?: specVersion ?: "unknown"),
                        "package" to pkg.name
                    ))
                }
            }
        } catch (e: Exception) {
            dependencies.add(mapOf(
                "error" to "Failed to retrieve dependency information: ${e.message}"
            ))
        }

        return dependencies
    }
}
