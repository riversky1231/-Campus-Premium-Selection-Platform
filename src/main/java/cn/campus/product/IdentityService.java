package cn.campus.product;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import java.time.Duration;
@Service
public class IdentityService {
 @Value("${market.auth-url:http://localhost:8081/api/users/identity}") String authUrl;
 public record Identity(String userId,String role){}
 private final RestClient http;
 public IdentityService(){var factory=new org.springframework.http.client.JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());factory.setReadTimeout(Duration.ofSeconds(5));http=RestClient.builder().requestFactory(factory).build();}
 public Identity identity(String token){if(token==null||!token.startsWith("Bearer "))throw Errors.fail(401,"请先登录");if(authUrl.isBlank())throw Errors.fail(503,"用户服务未配置");try{var user=http.get().uri(authUrl).header("Authorization",token).retrieve().body(Identity.class);if(user==null||user.userId()==null||!user.userId().matches("[0-9]{1,19}"))throw Errors.fail(401,"登录无效");return user;}catch(org.springframework.web.client.HttpClientErrorException e){throw Errors.fail(e.getStatusCode().value()==403?403:401,"登录无效或账号被停用");}catch(org.springframework.web.server.ResponseStatusException e){throw e;}catch(Exception e){throw Errors.fail(503,"用户服务暂不可用");}}
}
