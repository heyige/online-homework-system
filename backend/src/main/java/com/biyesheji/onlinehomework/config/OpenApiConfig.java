package com.biyesheji.onlinehomework.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 文档配置类
 * 配置 Swagger/OpenAPI 3.0 文档，用于 API 文档的自动生成和测试界面
 * 访问地址：http://localhost:8080/swagger-ui.html
 *
 * @author毕业设计
 * @version1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * 服务器端口号，从 application.yml 配置文件中读取
     * 默认值为 8080
     */
    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 创建自定义的 OpenAPI 配置 Bean
     * 配置 API 文档的标题、版本、描述、服务器信息和安全机制
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // 定义安全方案名称，用于后续引用
        final String securitySchemeName = "bearerAuth";

        // 构建并返回 OpenAPI 配置对象
        return new OpenAPI()
                // 配置 API 文档基本信息
                .info(new Info()
                        // API 文档标题
                        .title("在线作业管理系统 API")
                        // API 版本号
                        .version("1.0.0")
                        // API 详细描述
                        .description("基于 Spring Boot + Vue 的在线作业管理系统 RESTful API 文档")
                        // 联系人信息
                        .contact(new Contact()
                                .name("毕业设计")
                                .email("support@example.com"))
                        // API 许可证信息
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                // 配置 API 服务器列表
                .servers(List.of(
                        // 本地开发服务器
                        new Server()
                                // 服务器 URL，包含端口号
                                .url("http://localhost:" + serverPort + "/api")
                                // 服务器描述
                                .description("本地开发服务器")))
                // 添加全局安全要求
                // 所有 API 接口默认都需要认证（除非特别排除）
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                // 配置安全方案定义
                .components(new Components()
                        // 添加 Bearer Token 安全方案
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        // 安全方案名称
                                        .name(securitySchemeName)
                                        // 安全方案类型：HTTP 协议
                                        .type(SecurityScheme.Type.HTTP)
                                        // 认证协议：Bearer
                                        .scheme("bearer")
                                        // Token 格式：JWT
                                        .bearerFormat("JWT")
                                        // 安全方案描述
                                        .description("JWT Token")));
    }
}