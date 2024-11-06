package kai9.com.configuration;

//import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//@Configuration  //別居版は、同一サイトとして動くのでCORS不要。このソース自体削除してOKだが同居版用に未使用で残しておく(アノテーションをコメントアウトする事で無効化)
public class WebConfiguration implements WebMvcConfigurer {

    // CORS設定
    // https://b1san-blog.com/post/spring/spring-cors/
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // allowedOriginsを指定しなければ全てのサイトからの通信がOKになってしまう
                .allowedOrigins("https://kai9.com_dumy/") // 別居版は、同一サイトとして動くのでCORS不要
                // .allowedOrigins("https://kai9.com/")
//		      .allowedOrigins("https://localhost:9443/")
//		      .allowedOrigins("https://localhost/")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
        // .maxAge(3600);
    }

}