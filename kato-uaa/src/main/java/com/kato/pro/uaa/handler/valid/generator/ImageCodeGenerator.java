package com.kato.pro.uaa.handler.valid.generator;

import com.kato.pro.uaa.entity.code.ImageCode;
import com.kato.pro.uaa.properties.SecurityProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.context.request.ServletWebRequest;

import javax.annotation.Resource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * author: ZGF
 * context : 生成图片验证码
 */
@Component(value = "imageCodeGenerator")
public class ImageCodeGenerator implements ValidateCodeGenerator {

    @Resource
    private SecurityProperties securityProperties;

    @Override
    public ImageCode generateCode(ServletWebRequest request) {
        int width = ServletRequestUtils.getIntParameter(request.getRequest(),"width", 100);
        int height = ServletRequestUtils.getIntParameter(request.getRequest(),"height", 23);

        BufferedImage image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();

        Random random = new Random();

        g.setColor(getRandColor(200,500));
        g.fillRect(0,0,width,height);
        g.setFont(new Font("Times New Roman",Font.ITALIC,20));
        g.setColor(getRandColor(160,200));
        for(int i=0;i<155;i++){
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int x1 = random.nextInt(12);
            int y1 = random.nextInt(12);
            g.drawLine(x,y,x+x1,y+y1);
        }

        StringBuilder sRand = new StringBuilder();
        int length = securityProperties.getValid().getValidateCodeLength();
        for(int i = 0; i < length; i++){
            String rand = String.valueOf(random.nextInt(10));
            sRand.append(rand);
            g.setColor(new Color(20 + random.nextInt(110),20 + random.nextInt(110),20 + random.nextInt(110)));
            g.drawString(rand,13 * i + 6 ,16);
        }
        g.dispose();
        return new ImageCode(image, sRand.toString(), securityProperties.getValid().getExpireTimeLong());
    }

    private Color getRandColor(int fc,int bc) {
        Random random = new Random();
        if (fc > 255) {
            fc = 255;
        }
        if (bc > 255){
            bc = 255;
        }
        int r = fc + random.nextInt(bc - fc);
        int g = fc + random.nextInt(bc - fc);
        int b = fc + random.nextInt(bc - fc);
        return  new Color(r,g,b);
    }

}
