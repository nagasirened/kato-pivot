package com.kato.pro.serial;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @ClassName JsonSerializer
 * @Author Zeng Guangfu
 * @Description TODO
 * @Date 2022/4/1 4:07 下午
 * @Version 1.0
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
            throw new RuntimeException("HESSIAN序列化失败");
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data)){
            HessianSerializerInput hsi = new HessianSerializerInput(bis);
            return (T) hsi.readObject(clazz);
        } catch (Exception e) {
            throw new RuntimeException("HESSIAN反序列化失败");
        }
    }
}
