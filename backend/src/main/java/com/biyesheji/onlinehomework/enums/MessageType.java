package com.biyesheji.onlinehomework.enums;

/**
 * 消息类型枚举
 *
 * 描述：定义系统中消息/通知的类型
 * 用于区分不同业务场景下产生的消息
 *
 * 使用方式：
 * MessageType.HOMEWORK_CREATE.getCode()  // 返回 "homework_create"
 * MessageType.HOMEWORK_CREATE.getDescription()  // 返回 "作业创建"
 */
public enum MessageType {

    /**
     * 作业创建
     * 当教师创建新作业时发送的通知类型
     */
    HOMEWORK_CREATE("homework_create", "作业创建"),

    /**
     * 作业更新
     * 当教师更新作业内容时发送的通知类型
     */
    HOMEWORK_UPDATE("homework_update", "作业更新"),

    /**
     * 作业删除
     * 当教师删除作业时发送的通知类型
     */
    HOMEWORK_DELETE("homework_delete", "作业删除"),

    /**
     * 作业批改完成
     * 当教师批改学生作业后发送的通知类型
     */
    SUBMISSION_GRADED("submission_graded", "作业批改"),

    /**
     * 系统通知
     * 系统级别的通知消息
     */
    SYSTEM("system", "系统通知"),

    /**
     * 永久消息
     * 不会过期的永久性消息
     */
    PERMANENT("permanent", "永久消息"),

    /**
     * 定时公告
     * 定时发送的公告消息
     */
    SCHEDULED("scheduled", "定时公告");

    /**
     * 类型的代码标识
     * 用于数据库存储和程序判断
     */
    private final String code;

    /**
     * 类型的中文描述
     * 用于前端展示
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code 类型代码
     * @param description 中文描述
     */
    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取类型代码
     *
     * @return 类型代码字符串
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取中文描述
     *
     * @return 中文描述字符串
     */
    public String getDescription() {
        return description;
    }
}
