package cn.campus.user;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
@Service
public class WalletService {
 private final UserStore store;
 public WalletService(UserStore s){store=s;}
 public record Change(String requestId,long userId,BigDecimal amount){}
 @Transactional public Map<String,Object> change(Change in,String kind){
  var user=store.lock(in.userId());var old=store.jdbc().queryForList("SELECT user_id,amount,applied FROM wallet_operation WHERE request_id=? AND kind=?",in.requestId(),kind);
  if(!old.isEmpty()){var op=old.get(0);if(((Number)op.get("user_id")).longValue()!=in.userId()||((BigDecimal)op.get("amount")).compareTo(in.amount())!=0)throw Errors.fail(409,"请求号参数不匹配");return Map.of("balance",user.balance(),"applied",op.get("applied"));}
  boolean applied=true;BigDecimal next=user.balance();
  if(kind.equals("DEBIT")){
   if(!user.enabled())throw Errors.fail(403,"账号已停用");
   if(!store.jdbc().queryForList("SELECT request_id FROM wallet_operation WHERE request_id=? AND kind='REFUND'",in.requestId()).isEmpty())throw Errors.fail(409,"支付操作已取消");
   if(next.compareTo(in.amount())<0)throw Errors.fail(409,"余额不足");next=next.subtract(in.amount());
  }else if(kind.equals("REFUND")){
   var debit=store.jdbc().queryForList("SELECT user_id,amount FROM wallet_operation WHERE request_id=? AND kind='DEBIT'",in.requestId());
   if(debit.isEmpty())applied=false;
   else{var d=debit.get(0);if(((Number)d.get("user_id")).longValue()!=in.userId()||((BigDecimal)d.get("amount")).compareTo(in.amount())!=0)throw Errors.fail(409,"退款参数不匹配");next=next.add(in.amount());}
  }else next=next.add(in.amount());
  if(next.compareTo(new BigDecimal("9999999999.99"))>0)throw Errors.fail(409,"余额超出上限");
  store.jdbc().update("UPDATE app_user SET balance=? WHERE id=?",next,user.id());
  store.jdbc().update("INSERT INTO wallet_operation(request_id,kind,user_id,amount,applied) VALUES(?,?,?,?,?)",in.requestId(),kind,user.id(),in.amount(),applied);
  return Map.of("balance",next,"applied",applied);
 }
}
