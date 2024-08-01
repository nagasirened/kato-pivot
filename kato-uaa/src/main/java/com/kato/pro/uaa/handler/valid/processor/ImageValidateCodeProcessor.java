package com.kato.pro.uaa.handler.valid.processor;

import com.kato.pro.uaa.entity.code.ImageCode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;

import javax.imageio.ImageIO;
import java.io.IOException;

@Component("imageCodeProcessor")
public class ImageValidateCodeProcessor extends AbstractValidateCodeProcessor<ImageCode> {

    @Override
    void send(ServletWebRequest webRequest, ImageCode imageCode) throws IOException {
        ImageIO.write(imageCode.getImage(), "JPEG", webRequest.getResponse().getOutputStream());
    }

}
