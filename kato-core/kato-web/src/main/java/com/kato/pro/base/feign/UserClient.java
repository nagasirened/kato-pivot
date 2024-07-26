package com.kato.pro.base.feign;

import com.kato.pro.base.entity.LoginAppUser;
import com.kato.pro.base.entity.SysRole;
import com.kato.pro.base.entity.SysUser;
import com.kato.pro.base.feign.fallback.UserServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", fallbackFactory = UserServiceFallbackFactory.class, decode404 = true)
public interface UserClient {

    /**
     * feign rpc访问远程/users/{username}接口
     * 查询用户实体对象SysUser
     */
    @GetMapping(value = "/users/name/{username}")
    SysUser selectByUsername(@PathVariable("username") String username);

    /**
     * feign rpc访问远程/users-anon/login接口
     */
    @GetMapping(value = "/users-anon/login", params = "username")
    LoginAppUser findByUsername(@RequestParam("username") String username);

    /**
     * 通过手机号查询用户、角色信息
     */
    @GetMapping(value = "/users-anon/mobile", params = "mobile")
    LoginAppUser findByMobile(@RequestParam("mobile") String mobile);

    /**
     * 根据OpenId查询用户信息
     */
    @GetMapping(value = "/users-anon/openId", params = "openId")
    LoginAppUser findByOpenId(@RequestParam("openId") String openId);


    /**
     * 获取带角色的用户信息
     */
    @GetMapping(value = "/users/roleUser/{username}")
    SysUser selectRoleUser(@PathVariable("username") String username);

    /**
     * 获取用户的角色
     */
    @GetMapping("/users/{id}/roles")
    List<SysRole> findRolesByUserId(@PathVariable("id") Long id);

}
