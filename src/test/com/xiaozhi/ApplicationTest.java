package com.xiaozhi;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;


// Change to NONE to avoid starting a web server
@SpringBootTest
@WebAppConfiguration
public class ApplicationTest {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationTest.class);

    @Resource
    public void main(String[] args) throws Exception {
    }

}