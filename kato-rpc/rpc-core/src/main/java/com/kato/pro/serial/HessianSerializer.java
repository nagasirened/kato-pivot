package com.kato.pro.serial;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import com.kato.pro.common.exception.KatoServiceException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @ClassName JsonSerializer
 * @Author Zeng Guangfu
 */
@Slf4j
public class HessianSerializer implements Serializer {

    @Override
    public <T> byte[] serialize(T data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            HessianSerializerOutput hso = new HessianSerializerOutput(bos);
            hso.writeObject(data);
            hso.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new KatoServiceException("HESSIAN序列化失败");
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data)){
            HessianSerializerInput hsi = new HessianSerializerInput(bis);
            return (T) hsi.readObject(clazz);
        } catch (Exception e) {
            throw new KatoServiceException("HESSIAN反序列化失败");
        }
    }
}
