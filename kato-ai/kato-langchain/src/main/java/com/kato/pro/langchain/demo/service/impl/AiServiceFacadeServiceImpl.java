package com.kato.pro.langchain.demo.service.impl;

import com.kato.pro.langchain.demo.assistant.AiAssistant;
import com.kato.pro.langchain.demo.assistant.AiRegisterAssistant;
import com.kato.pro.langchain.demo.entity.IntentionOutput;
import com.kato.pro.langchain.demo.entity.LostRegisterOutput;
import com.kato.pro.langchain.demo.service.AiChatFacadeService;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiServiceFacadeServiceImpl implements AiChatFacadeService {

    @Autowired
    private AiAssistant aiAssistant;

    @Autowired
    private AiRegisterAssistant aiRegisterAssistant;

    @Override
    public String chatStream(String userId, String message) {
        IntentionOutput intention = aiAssistant.intention(userId, message);
        String output = intention.getOutput();
        switch (intention.getIntention()) {
            case 1:
                return registerLost(userId, message);
            case 2:
                log.info("找到失物登记");
                break;
            case 3:
                log.info("查询失物");
                break;
            default:
                return output;
        }
        return output;
    }

    public String registerLost(String userId, String message) {
        AiServices.builder(AiRegisterAssistant.class).build();
        LostRegisterOutput registerOutput = aiRegisterAssistant.registerLost(userId, message);
        // 如果完成登记，则保存失物信息
        if (registerOutput.getCompleted()) {
            // todo saveLost(registerOutput);
        }
        // 如果未完成登记，则返回大模型对用户的输出
        return registerOutput.getOutput();
    }

}
