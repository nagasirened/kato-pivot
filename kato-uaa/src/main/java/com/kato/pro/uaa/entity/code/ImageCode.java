package com.kato.pro.uaa.entity.code;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;

/**
 * author: ZGF
 * context : 图片验证码
 */

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ImageCode extends ValidateCode{

    private BufferedImage image;

    public ImageCode(BufferedImage image, String code, Integer expireIn) {
        super(code, expireIn);
        this.image = image;
    }

    public ImageCode(BufferedImage image, String code, LocalDateTime expire) {
        super(code, expire);
        this.image = image;
    }

}
