package com.biyesheji.onlinehomework.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时自动扩展 avatar 字段，兼容历史 Base64 头像数据。
 */
@Component
public class AvatarSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AvatarSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public AvatarSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateColumn("profile_requests");
        migrateColumn("users");
    }

    private void migrateColumn(String tableName) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE `" + tableName + "` MODIFY COLUMN `avatar` MEDIUMTEXT NULL"
            );
            log.info("Ensured {}.avatar column uses MEDIUMTEXT", tableName);
        } catch (Exception e) {
            log.warn("Skip avatar column migration for {}: {}", tableName, e.getMessage());
        }
    }
}
