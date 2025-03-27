package com.jinghu.cad.analysis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.jinghu.cad.analysis.mapper")
public class CadAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(CadAnalysisApplication.class, args);
    }

}
