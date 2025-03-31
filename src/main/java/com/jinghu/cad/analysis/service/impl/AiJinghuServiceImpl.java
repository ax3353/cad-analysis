package com.jinghu.cad.analysis.service.impl;

import com.jinghu.cad.analysis.mapper.AiJinghuMapper;
import com.jinghu.cad.analysis.service.IAiJinghuService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author liming
 * @version 1.0
 * @description AiJinghuServiceImpl
 * @date 2025/3/28 19:36
 */
@Service
public class AiJinghuServiceImpl implements IAiJinghuService {

    @Resource
    private AiJinghuMapper aiJinghuMapper;

    @Override
    public List<Map<Object, Object>> difySqlQuery(String querySql) {
        return aiJinghuMapper.difySqlQuery(querySql);
    }
}
