package com.ariescat.seckill.service;

import com.alibaba.druid.util.StringUtils;
import com.ariescat.seckill.bean.User;
import com.ariescat.seckill.exception.GlobalException;
import com.ariescat.seckill.mapper.UserMapper;
import com.ariescat.seckill.redis.RedisService;
import com.ariescat.seckill.redis.key.UserKeyPrefix;
import com.ariescat.seckill.result.CodeMsg;
import com.ariescat.seckill.util.MD5Util;
import com.ariescat.seckill.util.UUIDUtil;
import com.ariescat.seckill.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
public class UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    RedisService redisService;

    public static final String COOKIE_NAME_TOKEN = "token";

    public User getById(long id) {
        //对象缓存
        User user = redisService.get(UserKeyPrefix.getUserById, "" + id, User.class);
        if (user != null) {
            return user;
        }
        //取数据库
        user = userMapper.getById(id);
        //再存入缓存
        if (user != null) {
            redisService.set(UserKeyPrefix.getUserById, "" + id, user);
        }
        return user;
    }

    /**
     * 根据token获取用户信息
     */
    public User getByToken(HttpServletResponse response, String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        User user = redisService.get(UserKeyPrefix.getUserByToken, token, User.class);
        //延长有效期，有效期等于最后一次操作+有效期
        if (user != null) {
            addCookie(response, token, user);
        }
        return user;
    }

    /**
     * 用户登录, 要么处理成功返回true，否则会抛出全局异常
     * 抛出的异常信息会被全局异常接收，全局异常会将异常信息传递到全局异常处理器
     *
     * @param loginVo 封装了客户端请求传递过来的数据（即账号密码）
     *                （使用post方式，请求参数放在了请求体中，这个参数就是获取请求体中的数据）
     * @return 登录成功与否
     */
    public String login(HttpServletResponse response, LoginVo loginVo) {
        if (loginVo == null) {
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        String mobile = loginVo.getMobile();
        String formPass = loginVo.getPassword();
        //判断手机号是否存在
        User user = getById(Long.parseLong(mobile));
        if (user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        //验证密码
        String dbPass = user.getPassword();
        String saltDB = user.getSalt();
        String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
        if (!calcPass.equals(dbPass)) {
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }
        //生成唯一id作为token
        String token = UUIDUtil.uuid();
        addCookie(response, token, user);
        return token;
    }

    /**
     * 将token做为key，用户信息做为value 存入redis模拟session
     * 同时将token存入cookie，保存登录状态
     */
    public void addCookie(HttpServletResponse response, String token, User user) {
        redisService.set(UserKeyPrefix.getUserByToken, token, user);
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
        cookie.setMaxAge(UserKeyPrefix.getUserByToken.expireSeconds());
        cookie.setPath("/");//设置为网站根目录
        response.addCookie(cookie);
    }

    /**
     * 典型缓存同步场景：更新密码
     */
    public boolean updatePassword(String token, long id, String formPass) {
        // 取user
        User user = getById(id);
        if (user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        // 更新数据库
        User toBeUpdate = new User();
        toBeUpdate.setId(id);
        toBeUpdate.setPassword(MD5Util.formPassToDBPass(formPass, user.getSalt()));
        userMapper.updatePassword(toBeUpdate);
        // 更新缓存：先删除再插入
        // 如果不删除，以前的用户数据仍然存在于缓存中，则通过以前的token依旧可以访问到用户之前的数据，这会造成信息泄露
        redisService.delete(UserKeyPrefix.getUserById, "" + id);
        user.setPassword(toBeUpdate.getPassword());
        redisService.set(UserKeyPrefix.getUserByToken, token, user);
        return true;
    }
}
