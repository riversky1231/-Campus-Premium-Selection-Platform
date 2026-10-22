package cn.campus.user;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
@Component
public class Bootstrap implements ApplicationRunner {
 private final UserStore store;private final PasswordEncoder passwords;
 @Value("${market.demo:true}") boolean demo;
 @Value("${market.admin-password:}") String password;
 public Bootstrap(UserStore s,PasswordEncoder p){store=s;passwords=p;}
 public void run(ApplicationArguments args){
  if(store.name("admin")==null){String pw=password;if(pw.isBlank()){if(!demo)throw new IllegalStateException("首次启动请配置 MARKET_ADMIN_PASSWORD，至少12位");pw="AdminDemo123!";}if(pw.length()<12)throw new IllegalStateException("管理员密码至少12位");store.jdbc().update("INSERT INTO app_user(username,password_hash,nickname,role,balance,enabled) VALUES('admin',?,?,'ADMIN',0,true)",passwords.encode(pw),"校园管理员");}
  if(demo && store.name("student")==null)store.jdbc().update("INSERT INTO app_user(username,password_hash,nickname,role,balance,enabled) VALUES('student',?,?,'STUDENT',500,true)",passwords.encode("StudentDemo123!"),"演示同学");
 }
}
