/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.log4key.config.converter.ConfigConverter;
import com.log4key.config.converter.XmlConfigConverter;
import com.log4key.config.converter.PropertiesConfigConverter;
import com.log4key.config.converter.YamlConfigConverter;
import com.log4key.config.converter.ConfigConverterUtils;
import com.log4key.config.key.ConfigKey;
import com.log4key.config.resolver.ConfigResolver;
import com.log4key.internal.InternalLogger;

/**
 * Log4Key configuration loader.
 *
 * Log4Key配置加载器。
 */
public class Log4KeyConfigurationLoader {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(Log4KeyConfigurationLoader.class);

    /**
     * 默认配置文件常量
     */
    public static final String[] DEFAULT_CONFIG_FILES = {
            "log4key-default.xml",
            "log4key-default.properties",
            "log4key-default.yaml",
    };

    /**
     * 用户自定义配置文件常量
     */
    public static final String[] USER_CONFIG_FILES = {
            "log4key.xml",
            "log4key.properties",
            "log4key.yaml",
            "log4key.yml",
    };

    // ========================== public static 方法 ==========================

    /**
     * Loads configuration from a file path.
     *
     * 从文件加载配置。
     *
     * @param filePath the configuration file path / 配置文件路径
     * @return configuration properties map / 配置属性Map
     * @throws IOException if loading fails / 如果加载失败
     */
    public static Map<String, Object> loadFromFile(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Config file does not exist or is not a file: " + filePath);
        }

        // 根据文件扩展名选择加载方式
        String extension = getFileExtension(filePath);
        switch (extension.toLowerCase()) {
            case "properties":
                return loadFromPropertiesFile(file);
            case "xml":
                return loadFromXmlFile(file);
            case "yaml":
            case "yml":
                return loadFromYamlFile(file);
            default:
                throw new IOException("Unsupported config file format: " + extension);
        }
    }

    /**
     * 从XML输入流加载配置
     * @param inputStream XML输入流
     * @return 配置属性Map
     * @throws IOException 如果加载配置失败
     */
    public static Map<String, Object> loadFromXmlStream(InputStream inputStream) throws IOException {
        return parseStructuredXmlConfig(inputStream);
    }

    /**
     * 从输入流加载配置并返回ConfigResolver
     * @param inputStream 输入流
     * @param resourcePath 资源路径，用于确定文件格式
     * @return ConfigResolver
     * @throws IOException 如果加载配置失败
     */
    public static ConfigResolver loadConfigResolverFromInputStream(InputStream inputStream, String resourcePath) throws IOException {
        // 根据文件扩展名选择加载方式
        String extension = getFileExtension(resourcePath);
        try {
            // 使用新的ConfigConverter架构
            ConfigConverter converter = getConfigConverter(extension);
            if (converter != null) {
                // 使用ConfigConverter解析配置，直接返回ConfigResolver
                Map<ConfigKey<?>, Object> configKeyMap = converter.parse(inputStream);
                return new ConfigResolver(configKeyMap);
            }

            // 如果找不到对应的ConfigConverter，回退到原有逻辑
            switch (extension.toLowerCase()) {
                case "properties":
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    // 转换为Map<String, Object>
                    Map<String, Object> configMap = new HashMap<>();
                    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                        String key = (String) entry.getKey();
                        Object value = entry.getValue();
                        configMap.put(key, value);
                    }
                    // 转换为ConfigKey Map，然后创建ConfigResolver
                    // 使用ConfigConverterUtils将Map<String, Object>转换为Map<ConfigKey<?>, Object>
                    Map<ConfigKey<?>, Object> configKeyMap = ConfigConverterUtils.fromLegacyMap(configMap);
                    return new ConfigResolver(configKeyMap);
                case "xml":
                    // 对于XML，直接使用XmlConfigConverter
                    // 注意：输入流只能读取一次，所以需要重置或复制
                    // 这里我们直接使用XmlConfigConverter
                    XmlConfigConverter xmlConverter = new XmlConfigConverter();
                    Map<ConfigKey<?>, Object> xmlConfigKeyMap = xmlConverter.parse(inputStream);
                    return new ConfigResolver(xmlConfigKeyMap);
                case "yaml":
                case "yml":
                    // 从类路径加载YAML配置，处理SnakeYAML库不存在的情况
                    Map<String, Object> yamlConfigMap = loadYamlConfig(inputStream);
                    // 转换为ConfigKey Map
                    Map<ConfigKey<?>, Object> yamlConfigKeyMap = ConfigConverterUtils.fromLegacyMap(yamlConfigMap);
                    return new ConfigResolver(yamlConfigKeyMap);
                default:
                    throw new IOException("Unsupported config file format: " + extension + ". Supported formats: properties, xml, yaml, yml");
            }
        } catch (IOException e) {
            throw new IOException("Failed to parse config file " + resourcePath + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Unexpected error parsing config file " + resourcePath + ": " + e.getMessage(), e);
        }
    }

    /**
     * 从类路径加载默认配置文件，支持加载多个配置文件并合并，返回ConfigResolver
     * @return ConfigResolver
     */
    public static ConfigResolver loadDefaultConfigAsResolver() {
         ConfigResolver configResolver = ConfigResolver.EMPTY;

        // 1. 首先加载所有default配置文件（按顺序合并，后面的覆盖前面的）
        for (String configFile : DEFAULT_CONFIG_FILES) {
            // 尝试从类路径加载默认配置文件
            try (InputStream inputStream = Log4KeyConfigurationLoader.class.getClassLoader().getResourceAsStream(configFile)) {
                if (inputStream != null) {
                    ConfigResolver resolver = loadConfigResolverFromInputStream(inputStream, configFile);
                    configResolver = configResolver.merge(resolver);
                    printLog("INFO", "Loaded default config from classpath: " + configFile +
                            ", merged into ConfigResolver");
                }
            } catch (IOException e) {
                // 如果加载失败，尝试下一个配置文件
                printLog("DEBUG", "Failed to load default config from classpath: " + configFile + ": " + e.getMessage());
            }
        }

        // 2. 然后尝试加载用户自定义配置文件（优先级更高，会覆盖默认配置）
        // 只要成功加载一个用户配置文件，就停止加载其他用户配置文件
        boolean userConfigLoaded = false;
        for (String configFile : USER_CONFIG_FILES) {
            // 尝试从当前目录加载
            try {
                File currentDirFile = new File(configFile);
                if (currentDirFile.exists() && currentDirFile.isFile()) {
                    try (InputStream inputStream = Files.newInputStream(currentDirFile.toPath())) {
                        ConfigResolver resolver = loadConfigResolverFromInputStream(inputStream, configFile);
                        configResolver = configResolver.merge(resolver);
                        printLog("INFO", "Loaded user config from current directory: " + configFile);
                        userConfigLoaded = true;
                        break; // 成功加载一个用户配置文件，停止加载其他
                    }
                }
            } catch (IOException e) {
                // 忽略当前目录加载失败，继续尝试类路径加载
                printLog("DEBUG", "Failed to load user config from current directory: " + configFile + ": " + e.getMessage());
            }

            // 尝试从类路径加载
            try (InputStream inputStream = Log4KeyConfigurationLoader.class.getClassLoader().getResourceAsStream(configFile)) {
                if (inputStream != null) {
                    ConfigResolver resolver = loadConfigResolverFromInputStream(inputStream, configFile);
                    configResolver = configResolver.merge(resolver);
                    printLog("INFO", "Loaded user config from classpath: " + configFile);
                    userConfigLoaded = true;
                    break; // 成功加载一个用户配置文件，停止加载其他
                }
            } catch (IOException e) {
                // 如果加载失败，尝试下一个配置文件
                printLog("DEBUG", "Failed to load user config from classpath: " + configFile + ": " + e.getMessage());
            }
        }

        printLog("INFO", "Config loading completed. Loaded default config and user config if exists." +
                (userConfigLoaded ? " User config loaded successfully." : " No user config found, using default config only."));
        return configResolver;
    }

    // ========================== private static 方法 ==========================

    /**
     * 从Properties文件加载配置
     * @param file Properties文件
     * @return 配置属性Map
     * @throws IOException 如果加载配置失败
     */
    private static Map<String, Object> loadFromPropertiesFile(File file) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            properties.load(inputStream);
        }

        // 转换为Map<String, Object>
        Map<String, Object> configMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            configMap.put((String) entry.getKey(), entry.getValue());
        }

        return configMap;
    }

    /**
     * 从XML文件加载配置
     * @param file XML文件
     * @return 配置属性Map
     * @throws IOException 如果加载配置失败
     */
    private static Map<String, Object> loadFromXmlFile(File file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return parseStructuredXmlConfig(inputStream);
        }
    }

    /**
     * 解析结构化XML配置
     * @param inputStream XML输入流
     * @return 扁平化的配置Map
     * @throws IOException 如果解析失败
     */
    private static Map<String, Object> parseStructuredXmlConfig(InputStream inputStream) throws IOException {
        try {
            printLog("DEBUG", "Starting XML configuration parsing");

            // 第一阶段：解析XML文档
            org.w3c.dom.Document document = parseXmlDocument(inputStream);
            printLog("DEBUG", "XML document parsed successfully");

            // 第二阶段：验证XML配置
            validateXmlConfig(document);
            printLog("DEBUG", "XML configuration validated");

            // 第三阶段：构建配置Map
            Map<String, Object> configMap = new HashMap<>();

            String rootNodeName = document.getDocumentElement().getNodeName();
            printLog("DEBUG", "XML root node name: " + rootNodeName);

            if ("logkey".equals(rootNodeName)) {
                printLog("DEBUG", "Parsing new format XML config (<logkey> root)");
                // 解析新格式：<logkey>根节点，包含<configuration>、<formatters>、<appenders>、<loggers>子节点
                // 解析configuration节点（全局配置）
                parseConfiguration(document, configMap);

                // 解析formatters节点
                parseFormattersConfig(document, configMap);

                // 解析appenders节点
                parseAppendersConfig(document, configMap);

                // 解析loggers节点（包括root logger和非root logger）
                parseLoggersConfig(document, configMap);

                printLog("DEBUG", "New format XML config parsed successfully, config entries: " + configMap.size());
            } else if ("Log4KeyConfig".equals(rootNodeName)) {
                printLog("DEBUG", "Parsing old format XML config (<Log4KeyConfig> root)");
                // 解析旧格式：<Log4KeyConfig>根节点，全局配置直接在根节点属性中
                parseOldFormatXmlConfig(document, configMap);
                printLog("DEBUG", "Old format XML config parsed successfully, config entries: " + configMap.size());
            } else {
                printLog("ERROR", "Unknown XML root node: " + rootNodeName);
            }

            return configMap;
        } catch (Exception e) {
            printLog("ERROR", "Failed to parse XML config: " + e.getMessage());
            logger.error("Failed to parse XML config", e);
            throw new IOException("Failed to parse XML config: " + e.getMessage(), e);
        }
    }

    /**
     * 解析XML文档，禁用XXE和外部实体
     * @param inputStream XML输入流
     * @return 解析后的XML文档
     * @throws Exception 如果解析失败
     */
    private static org.w3c.dom.Document parseXmlDocument(InputStream inputStream) throws Exception {
        // 使用JDK原生DOM解析，禁用XXE和外部实体
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();

        // 禁用XXE和外部实体，提高安全性
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document document = builder.parse(inputStream);
        document.getDocumentElement().normalize();

        return document;
    }

    /**
     * 验证XML配置的基本结构
     * @param document XML文档
     */
    private static void validateXmlConfig(org.w3c.dom.Document document) {
        // 检查根节点是否为<logkey>或<Log4KeyConfig>（兼容旧格式）
        String rootNodeName = document.getDocumentElement().getNodeName();
        if (!"logkey".equals(rootNodeName) && !"Log4KeyConfig".equals(rootNodeName)) {
            throw new IllegalArgumentException("XML root element must be <logkey> or <Log4KeyConfig>, but got: <" + rootNodeName + ">");
        }
    }

    /**
     * 解析configuration节点，提取全局配置和默认值
     * @param document XML文档
     * @param configMap 配置Map
     */
    private static void parseConfiguration(org.w3c.dom.Document document, Map<String, Object> configMap) {
        org.w3c.dom.NodeList configurationNodes = document.getElementsByTagName("configuration");
        if (configurationNodes.getLength() > 0) {
            org.w3c.dom.Element configuration = (org.w3c.dom.Element) configurationNodes.item(0);

            // 解析configuration节点的属性
            org.w3c.dom.NamedNodeMap attributes = configuration.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                org.w3c.dom.Node attr = attributes.item(i);
                String attrName = attr.getNodeName();
                String attrValue = attr.getNodeValue();

                // 直接添加到配置Map中
                configMap.put(attrName, attrValue);
            }

            // 解析configuration节点的子元素
            org.w3c.dom.NodeList children = configuration.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node child = children.item(i);
                if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element childElement = (org.w3c.dom.Element) child;
                    String childName = childElement.getTagName();

                    // 特殊处理executor元素，它包含嵌套配置
                    if (ConfigKeys.EXECUTOR.equals(childName)) {
                        // 解析executor的子元素
                        org.w3c.dom.NodeList executorChildren = childElement.getChildNodes();
                        for (int j = 0; j < executorChildren.getLength(); j++) {
                            org.w3c.dom.Node executorChild = executorChildren.item(j);
                            if (executorChild.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                                org.w3c.dom.Element executorChildElement = (org.w3c.dom.Element) executorChild;
                                String executorChildName = executorChildElement.getTagName();
                                String executorChildValue = executorChildElement.getTextContent().trim();
                                String configKey = ConfigKeys.EXECUTOR + "." + executorChildName;
                                configMap.put(configKey, executorChildValue);
                            }
                        }
                    } else {
                        // 其他元素保持原有处理方式
                        String childValue = childElement.getTextContent().trim();
                        configMap.put(childName, childValue);
                    }
                }
            }
        }
    }

    /**
     * 解析loggers节点，包括root logger和非root logger配置
     * @param document XML文档
     * @param configMap 配置Map
     */
    private static void parseLoggersConfig(org.w3c.dom.Document document, Map<String, Object> configMap) {
        org.w3c.dom.NodeList loggersNodes = document.getElementsByTagName("loggers");
        if (loggersNodes.getLength() > 0) {
            org.w3c.dom.Element loggers = (org.w3c.dom.Element) loggersNodes.item(0);

            // 获取所有logger节点
            org.w3c.dom.NodeList loggerNodes = loggers.getChildNodes();
            for (int i = 0; i < loggerNodes.getLength(); i++) {
                org.w3c.dom.Node loggerNode = loggerNodes.item(i);
                if (loggerNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element logger = (org.w3c.dom.Element) loggerNode;
                    String loggerName = logger.getTagName();

                    if ("root".equals(loggerName)) {
                        // 解析root logger配置
                        parseRootLoggerConfig(logger, configMap);
                    } else if ("logger".equals(loggerName)) {
                        // 解析非root logger配置
                        parseLoggerConfig(logger, configMap);
                    } else {
                        // 未识别的logger类型，跳过并记录警告
                        printLog("WARN", "Unrecognized logger type: " + loggerName + ", skipping...");
                    }
                }
            }
        }
    }

    /**
     * 解析Root Logger配置
     * @param rootLogger Root Logger元素
     * @param configMap 配置Map
     */
    private static void parseRootLoggerConfig(org.w3c.dom.Element rootLogger, Map<String, Object> configMap) {
        printLog("DEBUG", "Parsing root logger configuration");

        if (rootLogger.hasAttribute("level")) {
            String level = rootLogger.getAttribute("level");
            configMap.put(ConfigKeys.ROOT_LOGGER_LEVEL, level);
            printLog("DEBUG", "Set rootLogger.level = " + level);
        }
        if (rootLogger.hasAttribute("appenders")) {
            String appenders = rootLogger.getAttribute("appenders");
            configMap.put(ConfigKeys.ROOT_LOGGER_APPENDERS, appenders);
            printLog("DEBUG", "Set rootLogger.appenders (from attribute) = " + appenders);
        }

        // 解析子元素作为属性（包括<appender-ref>元素）
        parseLoggerChildElements(rootLogger, "rootLogger", configMap);

        printLog("DEBUG", "Root logger configuration parsing completed");
    }

    /**
     * 解析非Root Logger配置
     * @param logger Logger元素
     * @param configMap 配置Map
     */
    private static void parseLoggerConfig(org.w3c.dom.Element logger, Map<String, Object> configMap) {
        String loggerName = logger.getAttribute("name");
        if (loggerName == null || loggerName.isEmpty()) {
            printLog("WARN", "Logger without name attribute, skipping...");
            return;
        }

        printLog("DEBUG", "Parsing logger configuration: " + loggerName);

        String loggerPrefix = "loggers." + loggerName + ".";

        if (logger.hasAttribute("level")) {
            String level = logger.getAttribute("level");
            configMap.put(loggerPrefix + ConfigKeys.LOGGER_LEVEL_SUFFIX, level);
            printLog("DEBUG", "Set " + loggerPrefix + ConfigKeys.LOGGER_LEVEL_SUFFIX + " = " + level);
        }
        if (logger.hasAttribute("appenders")) {
            String appenders = logger.getAttribute("appenders");
            configMap.put(loggerPrefix + ConfigKeys.LOGGER_APPENDERS_SUFFIX, appenders);
            printLog("DEBUG", "Set " + loggerPrefix + ConfigKeys.LOGGER_APPENDERS_SUFFIX + " (from attribute) = " + appenders);
        }

        // 解析子元素作为属性（包括<appender-ref>元素）
        parseLoggerChildElements(logger, loggerPrefix, configMap);

        printLog("DEBUG", "Logger configuration parsing completed: " + loggerName);
    }

    /**
     * 解析Logger的子元素
     * @param loggerElement Logger元素
     * @param loggerPrefix Logger配置前缀
     * @param configMap 配置Map
     */
    private static void parseLoggerChildElements(org.w3c.dom.Element loggerElement, String loggerPrefix, Map<String, Object> configMap) {
        org.w3c.dom.NodeList children = loggerElement.getChildNodes();
        List<String> appenderRefs = new ArrayList<>();

        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                org.w3c.dom.Element childElement = (org.w3c.dom.Element) child;
                String childName = childElement.getTagName();
                String childValue = childElement.getTextContent().trim();

                if ("appender-ref".equals(childName)) {
                    // 收集所有appender-ref值
                    if (!childValue.isEmpty()) {
                        appenderRefs.add(childValue);
                        printLog("DEBUG", "Found appender-ref for " + loggerPrefix + ": " + childValue);
                    }
                } else {
                    // 其他子元素正常处理
                    // 修复双点问题：如果loggerPrefix已以点结尾，则不再添加点
                    String key = loggerPrefix;
                    if (!key.endsWith(".")) {
                        key += ".";
                    }
                    key += childName;

                    logger.debug("XmlConfigLoader: "+key+"="+childValue);
                    configMap.put(key, childValue);
                    printLog("DEBUG", "Added logger config: " + key + " = " + childValue);
                }
            }
        }

        // 如果有appender-ref元素，合并为逗号分隔的字符串
        if (!appenderRefs.isEmpty()) {
            String appendersKey = loggerPrefix;
            if (!appendersKey.endsWith(".")) {
                appendersKey += ".";
            }
            appendersKey += ConfigKeys.LOGGER_APPENDERS_SUFFIX.replace(".", "");  // 使用.appenders而不是.appender-ref

            String appendersValue = String.join(",", appenderRefs);
            configMap.put(appendersKey, appendersValue);

            logger.debug("Configueration loader: " + appendersKey + "=" + appendersValue);

            printLog("DEBUG", "Added logger appenders config: " + appendersKey + " = " + appendersValue);
        }
    }

    /**
     * 解析Appenders配置
     * @param document XML文档
     * @param configMap 配置Map
     */
    private static void parseAppendersConfig(org.w3c.dom.Document document, Map<String, Object> configMap) {
        org.w3c.dom.NodeList appendersNodes = document.getElementsByTagName("appenders");
        if (appendersNodes.getLength() > 0) {
            org.w3c.dom.Element appenders = (org.w3c.dom.Element) appendersNodes.item(0);

            // 获取所有Appender节点
            org.w3c.dom.NodeList appenderNodes = appenders.getChildNodes();
            for (int i = 0; i < appenderNodes.getLength(); i++) {
                org.w3c.dom.Node appenderNode = appenderNodes.item(i);
                if (appenderNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element appender = (org.w3c.dom.Element) appenderNode;
                    String appenderName = appender.getAttribute("name");
                    String appenderType = appender.getTagName();

                    if (appenderName.isEmpty()) {
                        printLog("WARN", "Appender without name attribute, skipping...");
                        continue;
                    }

                    // 设置Appender类型
                    configMap.put(ConfigKeys.APPENDERS_PREFIX + appenderName + "." + ConfigKeys.APPENDER_TYPE, appenderType);

                    // 解析Appender的其他属性
                    parseAppenderAttributes(appender, appenderName, configMap);
                }
            }
        }
    }

    /**
     * 解析单个Appender的属性
     * @param appender Appender元素
     * @param appenderName Appender名称
     * @param configMap 配置Map
     */
    private static void parseAppenderAttributes(org.w3c.dom.Element appender, String appenderName, Map<String, Object> configMap) {
        String appenderPrefix = ConfigKeys.APPENDERS_PREFIX + appenderName + ".";

        // 解析所有属性
        org.w3c.dom.NamedNodeMap attributes = appender.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            org.w3c.dom.Node attr = attributes.item(i);
            String attrName = attr.getNodeName();
            String attrValue = attr.getNodeValue();

            // 跳过name属性，因为已经处理过
            if (!"name".equals(attrName)) {
                String key = appenderPrefix + attrName;
                configMap.put(key, attrValue);
            }
        }

        // 解析子元素
        org.w3c.dom.NodeList children = appender.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                org.w3c.dom.Element childElement = (org.w3c.dom.Element) child;
                String childName = childElement.getTagName();

                if ("formatter".equals(childName)) {
                    // 处理内联formatter或formatter引用
                    parseAppenderFormatter(childElement, appenderName, configMap);
                } else {
                    // 其他子元素作为属性
                    String childValue = childElement.getTextContent().trim();
                    String key = appenderPrefix + childName;
                    configMap.put(key, childValue);
                    printLog("DEBUG", "Set " + key + " = " + childValue);
                }
            }
        }
    }

    /**
     * 解析Appender的formatter配置（内联formatter或formatter引用）
     * @param formatterElement formatter元素
     * @param appenderName Appender名称
     * @param configMap 配置Map
     */
    private static void parseAppenderFormatter(org.w3c.dom.Element formatterElement, String appenderName, Map<String, Object> configMap) {
        String appenderPrefix = ConfigKeys.APPENDERS_PREFIX + appenderName + ".";
        logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: appender='" + appenderName + "', prefix='" + appenderPrefix + "'");

        if (formatterElement.hasAttribute("ref")) {
            // 处理formatter引用
            String formatterRef = formatterElement.getAttribute("ref");
            String formatterKey = appenderPrefix + ConfigKeys.APPENDER_FORMATTER;
            configMap.put(formatterKey, formatterRef);
            logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: added formatter reference key '" + formatterKey + "' = '" + formatterRef + "'");
        } else {
            // 处理内联formatter
            String inlineFormatterType = formatterElement.getTagName();
            String inlineFormatterName = appenderName + "_inline_formatter";
            logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: inline formatter - type='" + inlineFormatterType + "', name='" + inlineFormatterName + "'");

            // 设置内联formatter类型
            String typeKey = ConfigKeys.FORMATTERS_PREFIX + inlineFormatterName + "." + ConfigKeys.APPENDER_TYPE;
            configMap.put(typeKey, inlineFormatterType);
            logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: added inline formatter type key '" + typeKey + "' = '" + inlineFormatterType + "'");

            // 解析内联formatter的属性
            org.w3c.dom.NamedNodeMap attributes = formatterElement.getAttributes();
            logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: " + attributes.getLength() + " attributes");
            for (int i = 0; i < attributes.getLength(); i++) {
                org.w3c.dom.Node attr = attributes.item(i);
                String attrName = attr.getNodeName();
                String attrValue = attr.getNodeValue();
                logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: attribute '" + attrName + "' = '" + attrValue + "'");

                if (!"ref".equals(attrName)) {
                    String key = ConfigKeys.FORMATTERS_PREFIX + inlineFormatterName + "." + attrName;
                    configMap.put(key, attrValue);
                    logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: added attribute key '" + key + "' = '" + attrValue + "'");
                }
            }

            // 解析内联formatter的子元素
            org.w3c.dom.NodeList children = formatterElement.getChildNodes();
            logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: " + children.getLength() + " child nodes");
            int elementCount = 0;
            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node child = children.item(i);
                if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element childElement = (org.w3c.dom.Element) child;
                    String childName = childElement.getTagName();
                    String childValue = childElement.getTextContent().trim();
                    logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: child element '" + childName + "' = '" + childValue + "'");

                    String key = ConfigKeys.FORMATTERS_PREFIX + inlineFormatterName + "." + childName;
                    configMap.put(key, childValue);
                    logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: added child key '" + key + "' = '" + childValue + "'");
                    elementCount++;
                }
            }
            logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: added " + elementCount + " child elements for inline formatter");

            // 设置Appender使用内联formatter
            String appenderFormatterKey = appenderPrefix + ConfigKeys.APPENDER_FORMATTER;
            configMap.put(appenderFormatterKey, inlineFormatterName);
            logger.debug("[XML-PARSER-DEBUG] parseAppenderFormatter: set appender formatter key '" + appenderFormatterKey + "' = '" + inlineFormatterName + "'");
        }
    }

    /**
     * 解析旧格式XML配置（兼容旧版本）
     * @param document XML文档
     * @param configMap 配置Map
     */
    private static void parseOldFormatXmlConfig(org.w3c.dom.Document document, Map<String, Object> configMap) {
        org.w3c.dom.Element root = document.getDocumentElement();

        // 解析根节点属性作为全局配置
        org.w3c.dom.NamedNodeMap rootAttributes = root.getAttributes();
        for (int i = 0; i < rootAttributes.getLength(); i++) {
            org.w3c.dom.Node attr = rootAttributes.item(i);
            String attrName = attr.getNodeName();
            String attrValue = attr.getNodeValue();
            configMap.put(attrName, attrValue);
        }

        // 解析RootLogger配置
        org.w3c.dom.NodeList rootLoggerNodes = root.getElementsByTagName("RootLogger");
        if (rootLoggerNodes.getLength() > 0) {
            org.w3c.dom.Element rootLogger = (org.w3c.dom.Element) rootLoggerNodes.item(0);
            if (rootLogger.hasAttribute("level")) {
                configMap.put(ConfigKeys.ROOT_LOGGER_LEVEL, rootLogger.getAttribute("level"));
            }
            if (rootLogger.hasAttribute("appenders")) {
                configMap.put(ConfigKeys.ROOT_LOGGER_APPENDERS, rootLogger.getAttribute("appenders"));
            }
        }

        // 解析旧格式的Appenders配置
        org.w3c.dom.NodeList appendersNodes = root.getElementsByTagName("Appenders");
        if (appendersNodes.getLength() > 0) {
            org.w3c.dom.Element appenders = (org.w3c.dom.Element) appendersNodes.item(0);
            org.w3c.dom.NodeList appenderNodes = appenders.getChildNodes();
            for (int i = 0; i < appenderNodes.getLength(); i++) {
                org.w3c.dom.Node appenderNode = appenderNodes.item(i);
                if (appenderNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element appender = (org.w3c.dom.Element) appenderNode;
                    String appenderName = appender.getAttribute("name");
                    String appenderType = appender.getTagName();

                    if (appenderName == null || appenderName.isEmpty()) {
                        printLog("WARN", "Appender without name attribute, skipping...");
                        continue;
                    }

                    configMap.put(ConfigKeys.APPENDERS_PREFIX + appenderName + "." + ConfigKeys.APPENDER_TYPE, appenderType);

                    // 解析Appender属性
                    org.w3c.dom.NamedNodeMap attributes = appender.getAttributes();
                    for (int j = 0; j < attributes.getLength(); j++) {
                        org.w3c.dom.Node attr = attributes.item(j);
                        String attrName = attr.getNodeName();
                        String attrValue = attr.getNodeValue();
                        if (!"name".equals(attrName)) {
                            configMap.put(ConfigKeys.APPENDERS_PREFIX + appenderName + "." + attrName, attrValue);
                        }
                    }
                }
            }
        }

        // 解析旧格式的Formatters配置
        org.w3c.dom.NodeList formattersNodes = root.getElementsByTagName("Formatters");
        if (formattersNodes.getLength() > 0) {
            org.w3c.dom.Element formatters = (org.w3c.dom.Element) formattersNodes.item(0);
            org.w3c.dom.NodeList formatterNodes = formatters.getChildNodes();
            for (int i = 0; i < formatterNodes.getLength(); i++) {
                org.w3c.dom.Node formatterNode = formatterNodes.item(i);
                if (formatterNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element formatter = (org.w3c.dom.Element) formatterNode;
                    String formatterName = formatter.getAttribute("name");
                    String formatterType = formatter.getTagName();

                    if (formatterName == null || formatterName.isEmpty()) {
                        printLog("WARN", "Formatter without name attribute, skipping...");
                        continue;
                    }

                    configMap.put(ConfigKeys.FORMATTERS_PREFIX + formatterName + "." + ConfigKeys.APPENDER_TYPE, formatterType);

                    // 解析Formatter属性
                    org.w3c.dom.NamedNodeMap attributes = formatter.getAttributes();
                    for (int j = 0; j < attributes.getLength(); j++) {
                        org.w3c.dom.Node attr = attributes.item(j);
                        String attrName = attr.getNodeName();
                        String attrValue = attr.getNodeValue();
                        if (!"name".equals(attrName)) {
                            configMap.put(ConfigKeys.FORMATTERS_PREFIX + formatterName + "." + attrName, attrValue);
                        }
                    }
                }
            }
        }
    }

    /**
     * 解析Formatters配置
     * @param document XML文档
     * @param configMap 配置Map
     */
    private static void parseFormattersConfig(org.w3c.dom.Document document, Map<String, Object> configMap) {
        org.w3c.dom.NodeList formattersNodes = document.getElementsByTagName("formatters");

        if (formattersNodes.getLength() > 0) {
            org.w3c.dom.Element formatters = (org.w3c.dom.Element) formattersNodes.item(0);

            // 获取所有Formatter节点
            org.w3c.dom.NodeList formatterNodes = formatters.getChildNodes();

            int formatterCount = 0;
            for (int i = 0; i < formatterNodes.getLength(); i++) {
                org.w3c.dom.Node formatterNode = formatterNodes.item(i);
                if (formatterNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element formatter = (org.w3c.dom.Element) formatterNode;
                    String formatterName = formatter.getAttribute("name");
                    String formatterType = formatter.getTagName();

                    if (formatterName == null || formatterName.isEmpty()) {
                        printLog("WARN", "Formatter without name attribute, skipping...");
                        continue;
                    }

                    // 首先尝试从子元素<type>获取formatter类型
                    String actualFormatterType = formatterType; // 默认使用tagName

                    // 查找<type>子元素
                    org.w3c.dom.NodeList typeNodes = formatter.getElementsByTagName("type");
                    if (typeNodes.getLength() > 0) {
                        org.w3c.dom.Element typeElement = (org.w3c.dom.Element) typeNodes.item(0);
                        actualFormatterType = typeElement.getTextContent().trim();
                    }

                    // 设置Formatter类型
                    String typeKey = ConfigKeys.FORMATTERS_PREFIX + formatterName + "." + ConfigKeys.APPENDER_TYPE;
                    configMap.put(typeKey, actualFormatterType);

                    // 解析Formatter的其他属性
                    parseFormatterAttributes(formatter, formatterName, configMap);

                    formatterCount++;
                }
            }

        }
    }

    /**
     * 解析单个Formatter的属性
     * @param formatter Formatter元素
     * @param formatterName Formatter名称
     * @param configMap 配置Map
     */
    private static void parseFormatterAttributes(org.w3c.dom.Element formatter, String formatterName, Map<String, Object> configMap) {
        String formatterPrefix = ConfigKeys.FORMATTERS_PREFIX + formatterName + ".";

        // 解析所有属性
        org.w3c.dom.NamedNodeMap attributes = formatter.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            org.w3c.dom.Node attr = attributes.item(i);
            String attrName = attr.getNodeName();
            String attrValue = attr.getNodeValue();

            // 跳过name属性，因为已经处理过
            if (!"name".equals(attrName)) {
                String key = formatterPrefix + attrName;
                configMap.put(key, attrValue);
            }
        }

        // 解析子元素作为属性
        org.w3c.dom.NodeList children = formatter.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                org.w3c.dom.Element childElement = (org.w3c.dom.Element) child;
                String childName = childElement.getTagName();
                String childValue = childElement.getTextContent().trim();

                String key = formatterPrefix + childName;
                configMap.put(key, childValue);
            }
        }
    }

    /**
     * 从YAML文件加载配置
     * @param file YAML文件
     * @return 配置属性Map
     * @throws IOException 如果加载配置失败
     */
    private static Map<String, Object> loadFromYamlFile(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return loadYamlConfig(inputStream);
        }
    }

    /**
     * 加载YAML配置，处理SnakeYAML库不存在的情况
     * @param inputStream YAML输入流
     * @return 配置属性Map
     * @throws IOException 如果加载配置失败
     */
    private static Map<String, Object> loadYamlConfig(InputStream inputStream) throws IOException {
        try {
            // 动态加载SnakeYAML库，避免编译时依赖
            Class<?> yamlClass = Class.forName("org.yaml.snakeyaml.Yaml");
            Object yaml = yamlClass.getDeclaredConstructor().newInstance();

            // 调用load方法加载YAML配置
            Method loadMethod = yamlClass.getMethod("load", InputStream.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlMap = (Map<String, Object>) loadMethod.invoke(yaml, inputStream);

            // 将嵌套的YAML Map转换为扁平化的属性Map
            Map<String, Object> configMap = new HashMap<>();
            flattenYamlMap(yamlMap, "", configMap);

            return configMap;
        } catch (ClassNotFoundException e) {
            throw new IOException("SnakeYAML library not found. Please add SnakeYAML dependency to use YAML config files.", e);
        } catch (Exception e) {
            throw new IOException("Failed to load YAML config: " + e.getMessage(), e);
        }
    }

    /**
     * 将嵌套的YAML Map转换为扁平化的属性Map
     * @param yamlMap YAML Map
     * @param prefix 属性前缀
     * @param configMap 输出的配置Map
     */
    private static void flattenYamlMap(Map<String, Object> yamlMap, String prefix, Map<String, Object> configMap) {
        for (Map.Entry<String, Object> entry : yamlMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof Map) {
                // 递归处理嵌套Map
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenYamlMap(nestedMap, fullKey, configMap);
            } else {
                // 直接添加简单属性
                configMap.put(fullKey, value);
            }
        }
    }

    /**
     * 获取文件扩展名
     * @param filePath 文件路径
     * @return 文件扩展名
     */
    private static String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1);
    }

    /**
     * 根据文件扩展名获取对应的ConfigConverter
     * @param extension 文件扩展名
     * @return ConfigConverter实例，如果扩展名不支持则返回null
     */
    private static ConfigConverter getConfigConverter(String extension) {
        if (extension == null) {
            return null;
        }

        switch (extension.toLowerCase()) {
            case "xml":
                return new XmlConfigConverter();
            case "properties":
                return new PropertiesConfigConverter();
            case "yaml":
            case "yml":
                return new YamlConfigConverter();
            default:
                return null;
        }
    }

    /**
     * 打印日志信息
     * @param level 日志级别
     * @param message 日志消息
     */
    private static void printLog(String level, String message) {
        // 根据日志级别使用相应的InternalLogger方法
        switch (level.toUpperCase()) {
            case "DEBUG":
                logger.debug(message);
                break;
            case "INFO":
                logger.debug(message); // 使用debug级别，因为这些是内部配置加载信息
                break;
            case "WARN":
                logger.warn(message);
                break;
            case "ERROR":
                logger.warn(message); // 使用warn级别
                break;
            default:
                logger.debug(message);
        }
    }
}