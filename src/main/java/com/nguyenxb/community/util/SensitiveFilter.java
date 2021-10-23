package com.nguyenxb.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.Null;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

// 敏感词, 过滤器
@Component
public class SensitiveFilter{

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    // 敏感词替换符号
    private static final String REPLACEMENT = "***";

    // 根节点
    private TrieNode rootNode = new TrieNode();

    // 首次访问时,或在程序启动时初始化前缀树
    @PostConstruct // 初始化 注解, 当该类实例化后,调用构造器后,就自动调用这个方法
    public void init(){
        // 加载敏感词到内存
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null){
                // 将敏感词添加到前缀树
                this.addKeyword(keyword);
            }

        } catch (IOException e) {
            logger.error("加载敏感词文件失败:"+e.getMessage());
        }


    }

    /** 将一个敏感词添加到前缀树中
     *  实现思路:
     *      先将每一个敏感词拆分成一个字符,然后将敏感词
     * @param keyword 从文件中读取的一行敏感词
     */
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i=0; i<keyword.length(); i++){
            char ch = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(ch);


            // 初始化子节点
            if (subNode == null){
                subNode = new TrieNode();
                tempNode.addSubNode(ch,subNode);
            }

            // 指向下一个子节点,进行下一轮循环
            tempNode = subNode;

            // 设置结束标识
            if (i == keyword.length() -1){
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /** 过滤敏感词
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if (StringUtils.isBlank(text)){
            return null;
        }

        // 指针1 , 指向定义好的敏感词的前缀树
        TrieNode tempNode = rootNode;
        // 指针2 , 指向传入的文本的头字符
        int begin = 0;
        // 指针3, 指向传入文本的快指针
        int position = 0;
        // 结果:
        StringBuilder sb = new StringBuilder();

        while (position < text.length()){
            char ch = text.charAt(position);
            // 跳过特殊符号
            if (isSymbol(ch)){
                // 如果指针1 处于根节点,将此符号记录结果,并让指针2 向下走一步
                if (tempNode == rootNode){
                    sb.append(ch);
                    begin++;
                }
                // 无论符号在开头或者中间,指针3 都向下走
                position++;
                continue;
            }

            // 检查下级节点
            tempNode = tempNode.getSubNode(ch);
            if (tempNode == null){
                // 以begin位置开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                // 进入下一个位置
                position = ++begin;
                // 重新指向根节点
                tempNode = rootNode;
            }else if (tempNode.isKeywordEnd()){
                // 发现了敏感词, 将begin - position 的文本替换
                sb.append(REPLACEMENT);
                //进入下一个位置
                begin = ++position;
                // 重新指向根节点
                tempNode = rootNode;
            }else {
                // 检查下一个字符
                position++;
            }
        }

//        将最后一批字符计入结果
        sb.append(text.substring(begin));

        return sb.toString();

    }

    // 判断是否为符号
    private boolean isSymbol(Character ch){
        // 0x2E80 - 0x9FFF 是东亚文字范围

        return !CharUtils.isAsciiAlphanumeric(ch) && (ch < 0x2E80 || ch > 0x9FFF);
    }


    // 前缀树
    private class TrieNode{
        //关键词的结束标识
        private boolean isKeywordEnd = false;

        // 子节点(key 是下级字符,value是下级节点)
        private Map<Character,TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd(){
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd){
            isKeywordEnd = keywordEnd;
        }

        //添加子节点
        public void addSubNode(Character ch,TrieNode node){
            subNodes.put(ch,node);
        }

        // 获取子节点
        public TrieNode getSubNode(Character ch){
            return subNodes.get(ch);
        }
    }
}

