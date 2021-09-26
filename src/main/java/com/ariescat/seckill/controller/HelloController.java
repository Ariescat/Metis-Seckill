package com.ariescat.seckill.controller;

import com.ariescat.seckill.bean.User;
import com.ariescat.seckill.mapper.UserMapper;
import com.ariescat.seckill.redis.RedisService;
import com.ariescat.seckill.redis.key.UserKeyPrefix;
import com.ariescat.seckill.result.CodeMsg;
import com.ariescat.seckill.result.Result;
import com.ariescat.seckill.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloController {

    @Autowired
    UserService userService;
    @Autowired
    RedisService redisService;

    @Autowired
    UserMapper userMapper;

    @ResponseBody
    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    //1.rest api json输出 2.页面
    @RequestMapping("/hello")
    @ResponseBody
    public Result<String> hello() {
        return Result.success("hello, spring-boot!");
    }

    @ResponseBody
    @RequestMapping("/helloError")
    public Result<String> helloError() {
        return Result.error(CodeMsg.SERVER_ERROR);
    }

    @RequestMapping("/thymeleaf")
    public String thymeleaf(Model model) {
        model.addAttribute("name", "cat");
        return "hello";// 返回给客户端的html文件名
    }

    @RequestMapping("/db/get")
    @ResponseBody
    public Result<User> dbGet() {
        User user = userService.getById(1);
        return Result.success(user);
    }

    @ResponseBody
    @RequestMapping("/db/tx")
    public Result<Boolean> dbTX() {
        boolean tx = tx();
        return Result.success(tx);
    }

    @Transactional
    public boolean tx() {
        User user = new User();
        user.setId(1L);
        user.setNickname("cat");
        userMapper.insert(user);

        User user1 = new User();
        user1.setId(1L);
        user1.setNickname("dog");
        userMapper.insert(user1);

        return true;
    }


    // 测试RedisService的get方法
    @RequestMapping("/redis/get")
    @ResponseBody
    public Result<User> redisGet() {
        User user = redisService.get(UserKeyPrefix.getUserById, "" + 1, User.class);
        return Result.success(user);
    }

    // 测试RedisService的set方法
    @ResponseBody
    @RequestMapping("redis/set")
    public Result<Boolean> redisSet() {
        User user = new User();
        user.setId(1L);
        user.setNickname("caxacax");
        boolean set = redisService.set(UserKeyPrefix.getUserById, "" + 1, user);
        return Result.success(set);
    }

}
