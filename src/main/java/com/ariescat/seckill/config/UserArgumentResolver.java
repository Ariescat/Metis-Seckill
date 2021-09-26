package com.ariescat.seckill.config;

import com.ariescat.seckill.access.UserContext;
import com.ariescat.seckill.bean.User;
import com.ariescat.seckill.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 解析请求，并将请求的参数设置到方法参数中
 */
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    UserService userService;

    /**
     * 当参数类型为 User才做处理
     * <p>
     * 然后，该 Controller方法继续往下执行时所看到的 User 对象就是在这里的 resolveArgument()方法处理过的对象
     */
    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        //获取参数类型
        Class<?> clazz = methodParameter.getParameterType();
        return clazz == User.class;
    }

    /**
     * 思路：先获取到已有参数HttpServletRequest，从中获取到token，再用token作为key从redis拿到User，而HttpServletResponse作用是为了延迟有效期
     */
    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
//        HttpServletRequest request = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
//        HttpServletResponse response = nativeWebRequest.getNativeResponse(HttpServletResponse.class);
//
//        // 从请求对象中获取token（token可能有两种方式从客户端返回，1：通过url的参数，2：通过set-Cookie字段）
//        String paramToken = request.getParameter(UserService.COOKIE_NAME_TOKEN);
//        String cookieToken = getCookieValue(request, UserService.COOKIE_NAME_TOKEN);
//        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
//            return null;
//        }
//        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
//        return userService.getByToken(response, token);
        /**
         * threadlocal 存储线程副本 保证线程不冲突
         */
        return UserContext.getUser();
    }

}
