package com.kato.pro.rec.threadPool;

import cn.hutool.core.convert.Convert;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.*;

@SuppressWarnings("ALL")
public class MDCWrapperThreadPoolExecutor extends ThreadPoolExecutor {

    public MDCWrapperThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public MDCWrapperThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public MDCWrapperThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public MDCWrapperThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * wrap后的新线程，实际执行的是callable的call方法 ｜ runnable的run方法
     * 将MDC里面的参数数据发放到新的线程中
     */
    @Override
    public void execute(Runnable task) {
        super.execute(ThreadMdcWrapper.wrap(task, MDC.getCopyOfContextMap(), cloneRequestAttributes()));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(ThreadMdcWrapper.wrap(task, MDC.getCopyOfContextMap(), cloneRequestAttributes()));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(ThreadMdcWrapper.wrap(task, MDC.getCopyOfContextMap(), cloneRequestAttributes()));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(ThreadMdcWrapper.wrap(task, MDC.getCopyOfContextMap(), cloneRequestAttributes()), result);
    }

    private RequestAttributes cloneRequestAttributes() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        try {
            ServletRequestAttributes clonedRequestAttribute = new ServletRequestAttributes(new OnlyHeadersHttpServletRequestWrapper(requestAttributes.getRequest()), requestAttributes.getResponse());
            String[] scopeAttributes = requestAttributes.getAttributeNames(0);

            for(String attrName : scopeAttributes) {
                Object attrValue = requestAttributes.getAttribute(attrName, 0);
                if (attrValue != null) {
                    clonedRequestAttribute.setAttribute(attrName, attrValue, 0);
                }
            }
            return clonedRequestAttribute;
        } catch (Exception e) {
            return requestAttributes;
        }
    }

    private static class OnlyHeadersHttpServletRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> headers = new ConcurrentHashMap();

        public OnlyHeadersHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
            Enumeration<String> requestHeaderNames = request.getHeaderNames();
            if (requestHeaderNames != null) {
                while(requestHeaderNames.hasMoreElements()) {
                    String header = Convert.toStr(requestHeaderNames.nextElement());
                    this.headers.put(header, request.getHeader(header));
                }
            }
        }

        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(this.headers.keySet());
        }

        public String getHeader(String name) {
            return (String)this.headers.get(name);
        }
    }
}
