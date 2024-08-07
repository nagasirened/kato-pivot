package com.kato.pro.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1599282604110237521L;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 账号
     */
    private String account;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 昵称
     */
    private String nickName;
    /**
     * 租户ID
     */
    private String tenantId;
    /**
     * 部门id
     */
    private String deptId;
    /**
     * 岗位id
     */
    private String postId;
    /**
     * 角色id
     */
    private String roleId;
    /**
     * 角色名
     */
    private String roleName;

    /**
     * 登录类型
     */
    private int type;

}
