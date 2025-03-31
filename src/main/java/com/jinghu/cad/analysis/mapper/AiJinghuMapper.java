package com.jinghu.cad.analysis.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @author liming
 * @version 1.0
 * @description AiJinghuMapper
 * @date 2025/3/28 19:37
 */
public interface AiJinghuMapper {
    List<Map<Object,Object>> difySqlQuery(@Param("param") String querySql);
}
