package com.biyesheji.onlinehomework.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页数据封装 DTO
 *
 * 描述：用于封装分页查询的结果
 * 包含当前页数据、总记录数、总页数等信息
 *
 * @param <T> 分页数据的类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageDTO<T> {

    /**
     * 当前页的数据列表
     */
    private List<T> content;

    /**
     * 总记录数
     * 数据库中符合条件的总记录数
     */
    private long totalElements;

    /**
     * 总页数
     * 根据 totalElements 和 pageSize 计算得出
     */
    private int totalPages;

    /**
     * 当前页码
     * 从 1 开始计数
     */
    private int page;

    /**
     * 每页显示的记录数
     */
    private int size;
}
