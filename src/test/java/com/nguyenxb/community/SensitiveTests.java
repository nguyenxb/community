package com.nguyenxb.community;

import com.nguyenxb.community.util.SensitiveFilter;
import org.apache.commons.lang3.CharUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SensitiveTests {
    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void testSensitiveFilter(){
        String text = "来这,,..可以赌@#$%^&*(&^$%^&*博,,嫖$%^&娼$%^&*(GYHJMK<L,,你喜欢的都有,,吸.....毒";
         text = "&lt;h1&gt;嫖&gt;&gt;,.dasd娼jia+硪QQ:1231545645&lt;/h1&gt;";
        String t = sensitiveFilter.filter(text);
        System.out.println(t);
    }

    @Test
    public void testASCII(){
        char ch = '&';
        boolean b =  !CharUtils.isAsciiAlphanumeric(ch) && (ch > 0x2E80 && ch < 0x9FFF);
        System.out.println(b);

    }
}
