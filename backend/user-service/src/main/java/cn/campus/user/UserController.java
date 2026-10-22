package cn.campus.user;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.math.BigDecimal;
@RestController
public class UserController {
 final UserStore store;final AuthService auth;final WalletService wallet;
 public UserController(UserStore s,AuthService a,WalletService w){store=s;auth=a;wallet=w;}
 public record Register(@NotBlank @Pattern(regexp="[A-Za-z0-9_]{4,32}") String username,@NotBlank @Size(min=8,max=64) String password,@NotBlank @Size(max=60) String nickname){}
 public record Login(@NotBlank @Size(max=64) String username,@NotBlank @Size(max=64) String password){}
 public record WxCode(@NotBlank @Size(max=256) String code){}
 public record Nickname(@NotBlank @Size(max=60) String nickname){}
 public record Address(@NotBlank @Size(max=40) String recipient,@NotBlank @Pattern(regexp="[0-9+ -]{6,20}") String phone,@NotBlank @Size(max=250) String detail){}
 public record Money(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,80}") String requestId,@Positive long userId,@NotNull @DecimalMin("0.01") @DecimalMax("100000.00") @Digits(integer=6,fraction=2) BigDecimal amount){}
 public record Enabled(@NotNull Boolean enabled){}
 @PostMapping("/api/auth/register") public AuthService.Credentials register(@Valid @RequestBody Register in){return auth.register(in.username(),in.password(),in.nickname());}
 @PostMapping("/api/auth/login") public AuthService.Credentials login(@Valid @RequestBody Login in){return auth.login(in.username(),in.password());}
 @PostMapping("/api/auth/wx-login") public AuthService.Credentials wx(@Valid @RequestBody WxCode in){return auth.wxLogin(in.code());}
 @PostMapping("/api/auth/logout") public Map<String,Object> logout(@RequestHeader("Authorization") String token){auth.logout(token);return Map.of("success",true);}
 @GetMapping({"/api/users/me","/api/users/identity"}) public Map<String,Object> me(){return store.profile(store.id(SecurityConfig.current().id()));}
 @PutMapping("/api/users/me") public Map<String,Object> nickname(@Valid @RequestBody Nickname in){store.jdbc().update("UPDATE app_user SET nickname=? WHERE id=?",in.nickname(),SecurityConfig.current().id());return me();}
 @GetMapping("/api/users/addresses") public List<Map<String,Object>> addresses(){return store.rows("SELECT id,recipient,phone,detail FROM user_address WHERE user_id=? ORDER BY id DESC",SecurityConfig.current().id());}
 @PostMapping("/api/users/addresses") public Map<String,Object> address(@Valid @RequestBody Address in){long uid=SecurityConfig.current().id();var key=new org.springframework.jdbc.support.GeneratedKeyHolder();store.jdbc().update(c->{var st=c.prepareStatement("INSERT INTO user_address(user_id,recipient,phone,detail) VALUES(?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);st.setLong(1,uid);st.setString(2,in.recipient());st.setString(3,in.phone());st.setString(4,in.detail());return st;},key);return Map.of("id",Objects.requireNonNull(key.getKey()).longValue());}
 @PutMapping("/api/users/addresses/{id}") public Map<String,Object> address(@PathVariable long id,@Valid @RequestBody Address in){if(store.jdbc().update("UPDATE user_address SET recipient=?,phone=?,detail=? WHERE id=? AND user_id=?",in.recipient(),in.phone(),in.detail(),id,SecurityConfig.current().id())==0)throw Errors.fail(404,"地址不存在");return Map.of("id",id);}
 @DeleteMapping("/api/users/addresses/{id}") public Map<String,Object> deleteAddress(@PathVariable long id){if(store.jdbc().update("DELETE FROM user_address WHERE id=? AND user_id=?",id,SecurityConfig.current().id())==0)throw Errors.fail(404,"地址不存在");return Map.of("deleted",id);}
 @GetMapping("/api/users/wallet") public Map<String,Object> balance(){return Map.of("balance",store.id(SecurityConfig.current().id()).balance());}
 @GetMapping("/api/users/wallet/ledger") public List<Map<String,Object>> ledger(@RequestParam(defaultValue="1") @Min(1) @Max(1000000) int page){return store.rows("SELECT request_id,kind,amount,applied,created_at FROM wallet_operation WHERE user_id=? ORDER BY created_at DESC,request_id LIMIT 50 OFFSET ?",SecurityConfig.current().id(),(page-1)*50);}
 @GetMapping("/api/admin/users") public List<Map<String,Object>> users(@RequestParam(defaultValue="1") @Min(1) @Max(1000000) int page){return store.jdbc().query("SELECT * FROM app_user ORDER BY id LIMIT 50 OFFSET ?",UserStore.MAPPER,(page-1)*50).stream().map(store::profile).toList();}
 @PutMapping("/api/admin/users/{id}/enabled") public Map<String,Object> enabled(@PathVariable long id,@Valid @RequestBody Enabled in){if(id==SecurityConfig.current().id())throw Errors.fail(409,"不能停用自己");store.id(id);store.jdbc().update("UPDATE app_user SET enabled=? WHERE id=?",in.enabled(),id);return store.profile(store.id(id));}
 @PostMapping("/api/admin/wallet/recharge") public Map<String,Object> recharge(@Valid @RequestBody Money in){return wallet.change(new WalletService.Change(in.requestId(),in.userId(),in.amount()),"CREDIT");}
 @PostMapping("/api/internal/wallet/debit") public Map<String,Object> debit(@Valid @RequestBody Money in){return wallet.change(new WalletService.Change(in.requestId(),in.userId(),in.amount()),"DEBIT");}
 @PostMapping("/api/internal/wallet/refund") public Map<String,Object> refund(@Valid @RequestBody Money in){return wallet.change(new WalletService.Change(in.requestId(),in.userId(),in.amount()),"REFUND");}
 @GetMapping("/api/internal/users/{uid}/addresses/{id}") public Map<String,Object> internalAddress(@PathVariable long uid,@PathVariable long id){var rows=store.rows("SELECT id,recipient,phone,detail FROM user_address WHERE user_id=? AND id=?",uid,id);if(rows.isEmpty())throw Errors.fail(404,"地址不存在");return rows.get(0);}
}
