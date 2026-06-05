-- 修复头像字段长度不足：Base64 头像数据远超 varchar(255)
ALTER TABLE `profile_requests`
  MODIFY COLUMN `avatar` MEDIUMTEXT NULL COMMENT '头像';

ALTER TABLE `users`
  MODIFY COLUMN `avatar` MEDIUMTEXT NULL COMMENT '头像 URL 或 Base64';
