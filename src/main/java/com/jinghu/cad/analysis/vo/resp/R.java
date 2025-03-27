package com.jinghu.cad.analysis.vo.resp;

import lombok.Data;

/**
 * @author liming
 * @version 1.0
 * @description R
 * @date 2025/3/24 11:15
 */
@Data
public class R<T> {
    private int code;    // 状态码
    private String msg;  // 提示信息
    private T data;      // 返回数据

    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 成功返回
    public static <T> R<T> success(T data) {
        return new R<>(200, "操作成功", data);
    }

    public static <T> R<T> success(String msg, T data) {
        return new R<>(200, msg, data);
    }

    // 失败返回
    public static <T> R<T> error(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public static <T> R<T> error(String msg) {
        return new R<>(500, msg, null);
    }
}