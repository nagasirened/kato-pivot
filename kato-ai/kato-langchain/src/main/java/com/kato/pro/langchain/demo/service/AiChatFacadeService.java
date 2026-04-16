package com.kato.pro.langchain.demo.service;

public interface AiChatFacadeService {

    /**
     * 主要针对用户意图进行流程编排
     */
    String chatStream(String userId, String message);

}
