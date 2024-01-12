package com.kato.pro.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.*;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.schema.ScalarType;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@EnableOpenApi
@Configuration
public class Swagger3Config implements WebMvcConfigurer {

	@Bean
	public Docket docket() {
		return new Docket(DocumentationType.OAS_30)     //文档版本
				.pathMapping("/") 			// path 路径
				.enable(true)				// 开关
				.apiInfo(apiInfo())         //将api的元信息设置为包含在json ResourceListing响应中。
				//.globalRequestParameters(getGlobalRequestParameters())
				.select()                               //选择哪些接口作为swagger的doc发布
				.apis(RequestHandlerSelectors.any())
				.paths(PathSelectors.any())
				.build()
				.securitySchemes(securitySchemes())
				.securityContexts(securityContexts())
				.protocols(new HashSet<>(Arrays.asList("http", "https")))   //支持的通讯协议集合
				;
	}

	//生成接口信息，包括标题、联系人等
	private ApiInfo apiInfo() {
		final ApiInfo build = new ApiInfoBuilder()
				.title("接口文档")
				.description("如有疑问，请联系开发者")
				.contact(new Contact("kato", "katouyi@foxmail.com", "katouyi@foxmail.com"))
				.version("1.0")
				.build();
		return build;
	}

	//生成全局通用参数
	private List<RequestParameter> getGlobalRequestParameters() {
		List<RequestParameter> parameters = new ArrayList<>();
		parameters.add(new RequestParameterBuilder()
				.name("Authorization")
				.description("登录令牌")
				.required(true)
				.in(ParameterType.HEADER)
				.query(q -> q.model(m -> m.scalarModel(ScalarType.STRING)))
				.required(false)
				.build());
		/*parameters.add(new RequestParameterBuilder()
				.name("version")
				.description("客户端的版本号")
				.required(true)
				.in(ParameterType.QUERY)
				.query(q -> q.model(m -> m.scalarModel(ScalarType.STRING)))
				.required(false)
				.build());*/
		return parameters;
	}

	//生成通用响应信息
	private List<Response> getGlobalResponseMessage() {
		List<Response> responseList = new ArrayList<>();
		responseList.add(new ResponseBuilder().code("404").description("找不到资源").build());
		return responseList;
	}



	private List<SecurityScheme> securitySchemes() {
		List<SecurityScheme> securitySchemes = new ArrayList<>();
		securitySchemes.add(new ApiKey("Authorization", "Authorization", "header"));
		return securitySchemes;
	}

	private List<SecurityContext> securityContexts() {
		List<SecurityContext> securityContexts = new ArrayList<>();
		securityContexts.add(SecurityContext.builder()
				.securityReferences(defaultAuth())
				.forPaths(PathSelectors.regex("^(?!auth).*$")).build());
		return securityContexts;
	}

	private List<SecurityReference> defaultAuth() {
		AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
		AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
		authorizationScopes[0] = authorizationScope;
		List<SecurityReference> securityReferences = new ArrayList<>();
		securityReferences.add(new SecurityReference("Authorization", authorizationScopes));
		return securityReferences;
	}
}
