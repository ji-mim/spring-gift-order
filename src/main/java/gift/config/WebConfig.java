package gift.config;

import gift.login.LoginArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginCheckInterceptor loginCheckInterceptor;
    private final LoginArgumentResolver loginArgumentResolver;

    public WebConfig(LoginCheckInterceptor loginCheckInterceptor, LoginArgumentResolver loginArgumentResolver) {
        this.loginCheckInterceptor = loginCheckInterceptor;
        this.loginArgumentResolver = loginArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheckInterceptor)
                .order(1)
                .addPathPatterns(
                        "/api/wishes/**",
                        "/css/**", "/*.ico", "/error"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginArgumentResolver);
        WebMvcConfigurer.super.addArgumentResolvers(resolvers);
    }

    @Bean
    public RestClient kakaoRestClient() {
        return RestClient.builder()
                .baseUrl("https://kauth.kakao.com")
                .build();
    }

}
