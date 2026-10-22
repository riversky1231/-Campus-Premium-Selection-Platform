package cn.campus.user;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import java.time.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Service
public class AuthService {
 private final UserStore store; private final PasswordEncoder passwords; private final SecureRandom random=new SecureRandom();
 @Value("${market.wx-appid:}") String appid;
 @Value("${market.wx-secret:}") String secret;
 public AuthService(UserStore s,PasswordEncoder p){store=s;passwords=p;}
 public record Credentials(String token,long expiresIn,Map<String,Object> user){}
 public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 Credentials session(UserStore.User user){byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);store.jdbc().update("INSERT INTO user_session(token_hash,user_id,expires_at) VALUES(?,?,?)",hash(token),user.id(),java.sql.Timestamp.from(Instant.now().plusSeconds(7200)));return new Credentials(token,7200,store.profile(user));}
 @Transactional public Credentials register(String username,String password,String nickname){if(store.name(username)!=null)throw Errors.fail(409,"账号已存在");store.jdbc().update("INSERT INTO app_user(username,password_hash,nickname,role,balance,enabled) VALUES(?,?,?,'STUDENT',0,true)",username,passwords.encode(password),nickname);return session(store.name(username));}
 public Credentials login(String username,String password){var user=store.name(username);if(user==null||!passwords.matches(password,user.passwordHash())||!user.enabled())throw Errors.fail(401,"账号或密码错误，或账号已停用");return session(user);}
 public UserStore.User authenticate(String header){if(header==null||!header.startsWith("Bearer ")||header.length()>512)throw Errors.fail(401,"请先登录");String digest=hash(header.substring(7));var ids=store.jdbc().queryForList("SELECT user_id FROM user_session WHERE token_hash=? AND expires_at>CURRENT_TIMESTAMP",Long.class,digest);if(ids.isEmpty())throw Errors.fail(401,"登录已过期");var user=store.id(ids.get(0));if(!user.enabled())throw Errors.fail(403,"账号已停用");return user;}
 public void logout(String header){if(header!=null&&header.startsWith("Bearer "))store.jdbc().update("DELETE FROM user_session WHERE token_hash=?",hash(header.substring(7)));}
 @Transactional public Credentials wxLogin(String code){
  if(appid.isBlank()||secret.isBlank())throw Errors.fail(503,"尚未配置微信小程序AppID与Secret");
  Map<?,?> response;
  try{var factory=new org.springframework.http.client.JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());factory.setReadTimeout(Duration.ofSeconds(5));response=org.springframework.web.client.RestClient.builder().requestFactory(factory).build().get().uri("https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code",appid,secret,code).retrieve().body(Map.class);}catch(Exception e){throw Errors.fail(503,"微信登录服务暂不可用");}
  if(response==null||!(response.get("openid") instanceof String openid)||openid.isBlank())throw Errors.fail(401,"微信登录凭证无效");
  var users=store.jdbc().query("SELECT * FROM app_user WHERE openid=?",UserStore.MAPPER,openid);
  if(users.isEmpty()){String name="wx_"+UUID.randomUUID().toString().replace("-","");store.jdbc().update("INSERT INTO app_user(username,password_hash,nickname,role,balance,enabled,openid) VALUES(?,?,?,'STUDENT',0,true,?)",name,passwords.encode(UUID.randomUUID().toString()),"微信用户",openid);users=List.of(store.name(name));}
  if(!users.get(0).enabled())throw Errors.fail(403,"账号已停用");return session(users.get(0));
 }
}
