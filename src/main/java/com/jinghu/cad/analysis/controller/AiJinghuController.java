package com.jinghu.cad.analysis.controller;

import com.jinghu.cad.analysis.service.IAiJinghuService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author liming
 * @version 1.0
 * @description AiJinghuController
 * @date 2025/3/28 19:34
 */
@RestController
public class AiJinghuController {

    @Resource
    private IAiJinghuService iAiJinghuService;

    @PostMapping("/difySqlQuery")
    public List<Map<Object,Object>> difySqlQuery(String querySql) throws Exception {
        querySql=querySql.replace("```sql","");
        querySql=querySql.replace("```","");
        List<Map<Object, Object>> maps = iAiJinghuService.difySqlQuery(querySql);
        return maps;
    }
}
