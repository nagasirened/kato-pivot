package com.kato.pro.langchain.chat.prompt;


import dev.langchain4j.model.input.structured.StructuredPrompt;
import lombok.Data;

@Data
@StructuredPrompt("根据中国{{legal}}法律，解答以下问题, {{question}}")
public class LegalPrompt {

    private String legal;

    private String question;

}
