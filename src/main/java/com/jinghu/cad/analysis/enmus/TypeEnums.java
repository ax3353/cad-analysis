package com.jinghu.cad.analysis.enmus;

/**
 * @author liming
 * @version 1.0
 * @description TypeEnums
 * @date 2025/3/26 13:00
 */
public enum TypeEnums {
    //类型 管道 法兰 楼层数 金属软管 法兰盖
    UNKNOWN("未知"),
    PIPE("管道"),
    FERRULE("法兰"),
    METAL_RUBBER("金属软管"),
    FERRULE_COVER("法兰盖"),
    FLOOR("楼层数");

    private final String type;

    TypeEnums(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
