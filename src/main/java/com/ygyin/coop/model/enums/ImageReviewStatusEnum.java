package com.ygyin.coop.model.enums;

import lombok.Getter;

/**
 * 图片审核状态枚举类
 */
@Getter
public enum ImageReviewStatusEnum {

    IN_REVIEW("审核中", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    private final String roleText;
    private final int val;

    ImageReviewStatusEnum(String roleText, int val) {
        this.roleText = roleText;
        this.val = val;
    }

    /**
     * 通过 value 查找枚举对象
     *
     * @param val
     * @return 枚举对象
     */
    public static ImageReviewStatusEnum getEnumByVal(Integer val) {
        // 如果 val 为空则返回空
        if (val == null)
            return null;

        // 遍历 枚举类，values() 返回数组包括所有枚举对象，查找 values 相等的枚举对象
        for (ImageReviewStatusEnum reviewStatusEnum : ImageReviewStatusEnum.values())
            if (reviewStatusEnum.val == val)
                return reviewStatusEnum;

        return null;
    }
}
