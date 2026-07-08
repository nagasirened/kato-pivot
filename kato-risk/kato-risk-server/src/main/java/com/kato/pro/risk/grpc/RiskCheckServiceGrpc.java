package com.kato.pro.risk.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.59.0)",
    comments = "Source: risk.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class RiskCheckServiceGrpc {

  private RiskCheckServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.kato.pro.risk.RiskCheckService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.kato.pro.risk.grpc.GrpcRiskRequest,
      com.kato.pro.risk.grpc.GrpcRiskResponse> getCheckMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "check",
      requestType = com.kato.pro.risk.grpc.GrpcRiskRequest.class,
      responseType = com.kato.pro.risk.grpc.GrpcRiskResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.kato.pro.risk.grpc.GrpcRiskRequest,
      com.kato.pro.risk.grpc.GrpcRiskResponse> getCheckMethod() {
    io.grpc.MethodDescriptor<com.kato.pro.risk.grpc.GrpcRiskRequest, com.kato.pro.risk.grpc.GrpcRiskResponse> getCheckMethod;
    if ((getCheckMethod = RiskCheckServiceGrpc.getCheckMethod) == null) {
      synchronized (RiskCheckServiceGrpc.class) {
        if ((getCheckMethod = RiskCheckServiceGrpc.getCheckMethod) == null) {
          RiskCheckServiceGrpc.getCheckMethod = getCheckMethod =
              io.grpc.MethodDescriptor.<com.kato.pro.risk.grpc.GrpcRiskRequest, com.kato.pro.risk.grpc.GrpcRiskResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "check"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kato.pro.risk.grpc.GrpcRiskRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kato.pro.risk.grpc.GrpcRiskResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RiskCheckServiceMethodDescriptorSupplier("check"))
              .build();
        }
      }
    }
    return getCheckMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.kato.pro.risk.grpc.GrpcGroovyTestRequest,
      com.kato.pro.risk.grpc.GrpcGroovyTestResult> getTestGroovyScriptMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "testGroovyScript",
      requestType = com.kato.pro.risk.grpc.GrpcGroovyTestRequest.class,
      responseType = com.kato.pro.risk.grpc.GrpcGroovyTestResult.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.kato.pro.risk.grpc.GrpcGroovyTestRequest,
      com.kato.pro.risk.grpc.GrpcGroovyTestResult> getTestGroovyScriptMethod() {
    io.grpc.MethodDescriptor<com.kato.pro.risk.grpc.GrpcGroovyTestRequest, com.kato.pro.risk.grpc.GrpcGroovyTestResult> getTestGroovyScriptMethod;
    if ((getTestGroovyScriptMethod = RiskCheckServiceGrpc.getTestGroovyScriptMethod) == null) {
      synchronized (RiskCheckServiceGrpc.class) {
        if ((getTestGroovyScriptMethod = RiskCheckServiceGrpc.getTestGroovyScriptMethod) == null) {
          RiskCheckServiceGrpc.getTestGroovyScriptMethod = getTestGroovyScriptMethod =
              io.grpc.MethodDescriptor.<com.kato.pro.risk.grpc.GrpcGroovyTestRequest, com.kato.pro.risk.grpc.GrpcGroovyTestResult>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "testGroovyScript"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kato.pro.risk.grpc.GrpcGroovyTestRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kato.pro.risk.grpc.GrpcGroovyTestResult.getDefaultInstance()))
              .setSchemaDescriptor(new RiskCheckServiceMethodDescriptorSupplier("testGroovyScript"))
              .build();
        }
      }
    }
    return getTestGroovyScriptMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RiskCheckServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RiskCheckServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RiskCheckServiceStub>() {
        @java.lang.Override
        public RiskCheckServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RiskCheckServiceStub(channel, callOptions);
        }
      };
    return RiskCheckServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RiskCheckServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RiskCheckServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RiskCheckServiceBlockingStub>() {
        @java.lang.Override
        public RiskCheckServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RiskCheckServiceBlockingStub(channel, callOptions);
        }
      };
    return RiskCheckServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RiskCheckServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RiskCheckServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RiskCheckServiceFutureStub>() {
        @java.lang.Override
        public RiskCheckServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RiskCheckServiceFutureStub(channel, callOptions);
        }
      };
    return RiskCheckServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * 同步风控检查
     * </pre>
     */
    default void check(com.kato.pro.risk.grpc.GrpcRiskRequest request,
        io.grpc.stub.StreamObserver<com.kato.pro.risk.grpc.GrpcRiskResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckMethod(), responseObserver);
    }

    /**
     * <pre>
     * Groovy 脚本单元测试
     * </pre>
     */
    default void testGroovyScript(com.kato.pro.risk.grpc.GrpcGroovyTestRequest request,
        io.grpc.stub.StreamObserver<com.kato.pro.risk.grpc.GrpcGroovyTestResult> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getTestGroovyScriptMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service RiskCheckService.
   */
  public static abstract class RiskCheckServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RiskCheckServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service RiskCheckService.
   */
  public static final class RiskCheckServiceStub
      extends io.grpc.stub.AbstractAsyncStub<RiskCheckServiceStub> {
    private RiskCheckServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RiskCheckServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RiskCheckServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 同步风控检查
     * </pre>
     */
    public void check(com.kato.pro.risk.grpc.GrpcRiskRequest request,
        io.grpc.stub.StreamObserver<com.kato.pro.risk.grpc.GrpcRiskResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Groovy 脚本单元测试
     * </pre>
     */
    public void testGroovyScript(com.kato.pro.risk.grpc.GrpcGroovyTestRequest request,
        io.grpc.stub.StreamObserver<com.kato.pro.risk.grpc.GrpcGroovyTestResult> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getTestGroovyScriptMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service RiskCheckService.
   */
  public static final class RiskCheckServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RiskCheckServiceBlockingStub> {
    private RiskCheckServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RiskCheckServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RiskCheckServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 同步风控检查
     * </pre>
     */
    public com.kato.pro.risk.grpc.GrpcRiskResponse check(com.kato.pro.risk.grpc.GrpcRiskRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Groovy 脚本单元测试
     * </pre>
     */
    public com.kato.pro.risk.grpc.GrpcGroovyTestResult testGroovyScript(com.kato.pro.risk.grpc.GrpcGroovyTestRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getTestGroovyScriptMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service RiskCheckService.
   */
  public static final class RiskCheckServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<RiskCheckServiceFutureStub> {
    private RiskCheckServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RiskCheckServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RiskCheckServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 同步风控检查
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.kato.pro.risk.grpc.GrpcRiskResponse> check(
        com.kato.pro.risk.grpc.GrpcRiskRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Groovy 脚本单元测试
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.kato.pro.risk.grpc.GrpcGroovyTestResult> testGroovyScript(
        com.kato.pro.risk.grpc.GrpcGroovyTestRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getTestGroovyScriptMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CHECK = 0;
  private static final int METHODID_TEST_GROOVY_SCRIPT = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CHECK:
          serviceImpl.check((com.kato.pro.risk.grpc.GrpcRiskRequest) request,
              (io.grpc.stub.StreamObserver<com.kato.pro.risk.grpc.GrpcRiskResponse>) responseObserver);
          break;
        case METHODID_TEST_GROOVY_SCRIPT:
          serviceImpl.testGroovyScript((com.kato.pro.risk.grpc.GrpcGroovyTestRequest) request,
              (io.grpc.stub.StreamObserver<com.kato.pro.risk.grpc.GrpcGroovyTestResult>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getCheckMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.kato.pro.risk.grpc.GrpcRiskRequest,
              com.kato.pro.risk.grpc.GrpcRiskResponse>(
                service, METHODID_CHECK)))
        .addMethod(
          getTestGroovyScriptMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.kato.pro.risk.grpc.GrpcGroovyTestRequest,
              com.kato.pro.risk.grpc.GrpcGroovyTestResult>(
                service, METHODID_TEST_GROOVY_SCRIPT)))
        .build();
  }

  private static abstract class RiskCheckServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RiskCheckServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.kato.pro.risk.grpc.Risk.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RiskCheckService");
    }
  }

  private static final class RiskCheckServiceFileDescriptorSupplier
      extends RiskCheckServiceBaseDescriptorSupplier {
    RiskCheckServiceFileDescriptorSupplier() {}
  }

  private static final class RiskCheckServiceMethodDescriptorSupplier
      extends RiskCheckServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    RiskCheckServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RiskCheckServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RiskCheckServiceFileDescriptorSupplier())
              .addMethod(getCheckMethod())
              .addMethod(getTestGroovyScriptMethod())
              .build();
        }
      }
    }
    return result;
  }
}
