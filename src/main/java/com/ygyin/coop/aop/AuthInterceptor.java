package com.ygyin.coop.aop;

import com.ygyin.coop.annotation.AuthVerify;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.UserRoleEnum;
import com.ygyin.coop.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 定义环绕切面，对有 authVerify 注解的方法进行拦截，校验权限
     *
     * @param joinPoint  切入点
     * @param authVerify 需要权限校验的注解
     * @return
     */
    @Around("@annotation(authVerify)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthVerify authVerify) throws Throwable {
        // 1. 通过注解中的字段，获取调用当前方法或接口所需的角色权限，并转化为对应枚举类
        String requiredRole = authVerify.requiredRole();
        UserRoleEnum requiredRoleEnum = UserRoleEnum.getEnumByVal(requiredRole);

        // 2. 获取 http servlet request 以获取当前登录用户
        RequestAttributes requestAttrs = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttrs).getRequest();
        // 通过 request 获取当前登录用户，并将当前用户权限转为枚举类
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum curRoleEnum = UserRoleEnum.getEnumByVal(loginUser.getUserRole());

        // 3. 检查权限，如果无需权限，则将当前请求直接放行
        if (requiredRoleEnum == null)
            return joinPoint.proceed();

        // 后续都需要权限，则先看当前用户的权限是否为空，为空着不放行抛业务异常
        ThrowUtils.throwIf(curRoleEnum == null, ErrorCode.NO_AUTH);

        // 当接口要求必须有管理员权限，但当前用户没有管理员权限，拒绝请求
        ThrowUtils.throwIf(UserRoleEnum.ADMIN.equals(requiredRoleEnum) && !UserRoleEnum.ADMIN.equals(curRoleEnum),
                ErrorCode.NO_AUTH);

        // 通过权限校验，放行该请求
        return joinPoint.proceed();
    }
}
