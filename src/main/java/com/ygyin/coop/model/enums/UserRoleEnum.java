package com.ygyin.coop.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户权限枚举类
 */
@Getter
public enum UserRoleEnum {

    USER("普通用户","user"),
    ADMIN("管理员", "admin");

    private final String roleText;
    private final String val;

    UserRoleEnum(String roleText, String val){
        this.roleText=roleText;
        this.val =val;
    }

    /**
     * 通过 value 查找枚举对象
     *
     * @param val
     * @return 枚举对象
     */
    public static UserRoleEnum getEnumByVal(String val) {
        // 如果 val 为空则返回空
        if (val.isEmpty())
            return null;

        // 遍历 枚举类，values() 返回数组包括所有枚举对象，查找 values 相等的枚举对象
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values())
            if (userRoleEnum.val.equals(val))
                return userRoleEnum;

        return null;
    }
}
