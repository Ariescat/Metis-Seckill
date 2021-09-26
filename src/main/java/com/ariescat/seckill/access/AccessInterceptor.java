package com.ariescat.seckill.access;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.ariescat.seckill.bean.User;
import com.ariescat.seckill.redis.RedisService;
import com.ariescat.seckill.result.CodeMsg;
import com.ariescat.seckill.result.Result;
import com.ariescat.seckill.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 用户访问拦截器，限制用户对某一个接口的频繁访问
 */
@Service
public class AccessInterceptor extends HandlerInterceptorAdapter {

    private static final Logger log = LoggerFactory.getLogger(AccessInterceptor.class);

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    /**
     * 目标方法执行前的处理
     * <p>
     * 查询访问次数，进行防刷请求拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // 指明拦截的是方法
        if (handler instanceof HandlerMethod) {
            User user = this.getUser(request, response); // 获取用户对象
            UserContext.setUser(user); // 保存用户到ThreadLocal，这样，同一个线程访问的是同一个用户

            HandlerMethod hm = (HandlerMethod) handler;
            log.info("拦截handler: {} ", hm);
            // 获取标注了@AccessLimit的方法，没有注解，则直接返回
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            // 如果没有添加@AccessLimit注解，直接放行（true）
            if (accessLimit == null) {
                return true;
            }

            // 获取注解的元素值
            int seconds = accessLimit.seconds();
            int maxCount = accessLimit.maxAccessCount();
            boolean needLogin = accessLimit.needLogin();

            String key = request.getRequestURI();
            if (needLogin) {
                if (user == null) {
                    this.render(response, CodeMsg.SESSION_ERROR);
                    return false;
                }
                key += ":" + user.getId();
            } else {
                //do nothing
            }
            AccessKey accessKey = AccessKey.withExpire(seconds);
            Integer count = redisService.get(accessKey, key, Integer.class);
            if (count == null) {
                // 第一次重复点击秒杀
                redisService.set(accessKey, key, 1);
            } else if (count < maxCount) {
                // 点击次数为未达最大值
                redisService.incr(accessKey, key);
            } else {
                // 点击次数已满
                this.render(response, CodeMsg.ACCESS_LIMIT_REACHED);
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        super.afterCompletion(request, response, handler, ex);
        UserContext.removeUser();
    }

    /**
     * 和 UserArgumentResolver 功能类似，用于解析拦截的请求，获取 User 对象
     */
    private User getUser(HttpServletRequest request, HttpServletResponse response) {
        String paramToken = request.getParameter(UserService.COOKIE_NAME_TOKEN);
        String cookieToken = getCookieValue(request, UserService.COOKIE_NAME_TOKEN);
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        return userService.getByToken(response, token);
    }

    /**
     * 点击次数已满后，向客户端反馈一个“频繁请求”提示信息
     */
    private void render(HttpServletResponse response, CodeMsg cm) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        OutputStream out = response.getOutputStream();
        String str = JSON.toJSONString(Result.error(cm));
        out.write(str.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }

    // 遍历所有cookie，找到需要的那个cookie
    private String getCookieValue(HttpServletRequest request, String cookiName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length <= 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookiName)) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
