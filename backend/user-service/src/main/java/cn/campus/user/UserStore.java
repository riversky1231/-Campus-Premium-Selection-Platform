package cn.campus.user;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;
@Repository
public class UserStore {
 private final JdbcTemplate db;
 public UserStore(JdbcTemplate db){this.db=db;}
 public List<Map<String,Object>> rows(String sql,Object... args){return db.queryForList(sql,args).stream().map(row->{Map<String,Object> result=new LinkedHashMap<>();row.forEach((key,value)->result.put(key.toLowerCase(Locale.ROOT),value));return result;}).toList();}
 public JdbcTemplate jdbc(){return db;}
 public record User(long id,String username,String passwordHash,String nickname,String role,BigDecimal balance,boolean enabled,String openid){}
 static final org.springframework.jdbc.core.RowMapper<User> MAPPER=(r,n)->new User(r.getLong("id"),r.getString("username"),r.getString("password_hash"),r.getString("nickname"),r.getString("role"),r.getBigDecimal("balance"),r.getBoolean("enabled"),r.getString("openid"));
 public User id(long id){return db.query("SELECT * FROM app_user WHERE id=?",MAPPER,id).stream().findFirst().orElseThrow(()->Errors.fail(404,"用户不存在"));}
 public User name(String name){return db.query("SELECT * FROM app_user WHERE username=?",MAPPER,name).stream().findFirst().orElse(null);}
 public User lock(long id){return db.query("SELECT * FROM app_user WHERE id=? FOR UPDATE",MAPPER,id).stream().findFirst().orElseThrow(()->Errors.fail(404,"用户不存在"));}
 public Map<String,Object> profile(User u){return Map.of("id",u.id(),"userId",String.valueOf(u.id()),"username",u.username(),"nickname",u.nickname(),"role",u.role(),"balance",u.balance(),"enabled",u.enabled());}
}
