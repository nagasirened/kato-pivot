package constant;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName RpcRequest
 * @Author Zeng Guangfu
 * @Description RPC调用请求
 * @Date 2022/4/1 9:52 上午
 * @Version 1.0
 */
@Data
public class RpcRequest implements Serializable {

    /**
     * serviceName + version
     */
    private String serviceVersion;

    /**
     * 方法名
     */
    private String method;

    /**
     * 方法参数类型集合
     */
    private Class<?>[] parameterTypes;

    /**
     * 参数集合
     */
    private Object[] parameters;
}
