package com.biyesheji.onlinehomework.repository;

import com.biyesheji.onlinehomework.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层接口
 *
 * 描述：负责与数据库中的 users 表进行交互
 * 继承 JpaRepository，自动提供基础的 CRUD 操作
 *
 * 自定义查询方法遵循 Spring Data JPA 命名规范：
 * - findBy + 字段名：按字段查询
 * - existsBy + 字段名：判断是否存在
 *
 * @Repository: 标记为 Spring 数据访问组件
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询用户
     * Spring Data JPA 会自动解析方法名并生成 SQL
     *
     * @param username 用户名
     * @return 包含用户的 Optional 对象
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 电子邮箱
     * @return 包含用户的 Optional 对象
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号码
     * @return 包含用户的 Optional 对象
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据角色查询用户列表（不区分大小写）
     *
     * @param role 用户角色 (ADMIN/TEACHER/STUDENT)
     * @return 符合条件用户的列表
     */
    List<User> findByRoleIgnoreCase(String role);

    /**
     * 判断用户名是否存在
     *
     * @param username 用户名
     * @return true 表示存在，false 表示不存在
     */
    boolean existsByUsername(String username);

    /**
     * 判断邮箱是否存在
     *
     * @param email 电子邮箱
     * @return true 表示存在，false 表示不存在
     */
    boolean existsByEmail(String email);

    /**
     * 判断手机号是否存在
     *
     * @param phone 手机号码
     * @return true 表示存在，false 表示不存在
     */
    boolean existsByPhone(String phone);

    /**
     * 多条件分页查询用户
     * 使用 JPQL 自定义查询语句
     *
     * @param username 用户名（模糊查询，可为 null）
     * @param name 姓名（模糊查询，可为 null）
     * @param role 角色（精确查询，可为 null）
     * @param pageable 分页参数
     * @return 分页结果
     *
     * 查询逻辑：
     * - 如果参数为 null 或空字符串，则该条件不生效（返回所有）
     * - username 和 name 使用 LIKE 模糊匹配
     * - role 使用精确匹配
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:username IS NULL OR :username = '' OR u.username LIKE %:username%) AND " +
           "(:name IS NULL OR :name = '' OR u.name LIKE %:name%) AND " +
           "(:role IS NULL OR :role = '' OR u.role = :role)")
    Page<User> findByFilters(@Param("username") String username,
                            @Param("name") String name,
                            @Param("role") String role,
                            Pageable pageable);
}
