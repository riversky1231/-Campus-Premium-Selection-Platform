package cn.campus.order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
@Service
public class OrderService {
 private final OrderStore store;private final ProductPort products;private final UserPort users;private final ObjectMapper json;
 @Value("${market.demo:true}") boolean demo;
 @Value("${market.order-timeout-seconds:900}") int timeout;
 public OrderService(OrderStore s,ProductPort p,UserPort u,ObjectMapper j){store=s;products=p;users=u;json=j;}
 public record Line(@Positive long productId,@Min(1) @Max(99) int quantity){}
 public record Create(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,80}") String requestId,@Positive long addressId,@NotEmpty @Size(max=20) List<@Valid Line> items){}
 String fingerprint(Create in){try{String value=in.addressId()+":"+in.items().stream().sorted(Comparator.comparingLong(Line::productId)).toList();return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 void event(String orderId,String type,Map<String,Object> payload){try{store.jdbc().update("INSERT INTO event_outbox(id,order_id,event_type,payload) VALUES(?,?,?,?)",UUID.randomUUID().toString(),orderId,type,json.writeValueAsString(payload));}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}}
 @Transactional
 public Map<String,Object> create(long uid,Create in){
  if(in.items().stream().map(Line::productId).distinct().count()!=in.items().size())throw Errors.fail(400,"商品不能重复");
  String fingerprint=fingerprint(in);var existing=store.jdbc().query("SELECT * FROM market_order WHERE user_id=? AND request_id=?",OrderStore.ROW,uid,in.requestId());if(!existing.isEmpty()){if(!existing.get(0).fingerprint().equals(fingerprint))throw Errors.fail(409,"请求号对应的订单参数不同");return store.detail(existing.get(0));}
  var address=users.address(uid,in.addressId());List<Contracts.Product> snapshots=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;
  for(Line line:in.items()){var product=products.product(line.productId());if(!Boolean.TRUE.equals(product.onSale()))throw Errors.fail(409,"商品已下架");snapshots.add(product);total=total.add(product.price().multiply(BigDecimal.valueOf(line.quantity())));}
  if(total.compareTo(new BigDecimal("100000.00"))>0)throw Errors.fail(400,"单笔订单金额超过限制");
  String id=UUID.randomUUID().toString();LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);LocalDateTime expires=now.plusSeconds(timeout);
  store.jdbc().update("INSERT INTO market_order(id,user_id,request_id,fingerprint,status,amount,recipient,phone,address_detail,created_at,expires_at) VALUES(?,?,?,?,'PENDING',?,?,?,?,?,?)",id,uid,in.requestId(),fingerprint,total,address.recipient(),address.phone(),address.detail(),java.sql.Timestamp.valueOf(now),java.sql.Timestamp.valueOf(expires));
  List<Contracts.Stock> attempted=new ArrayList<>();
  try{
   for(int i=0;i<in.items().size();i++){Line line=in.items().get(i);var product=snapshots.get(i);var change=new Contracts.Stock(id+"_"+line.productId(),line.productId(),line.quantity());attempted.add(change);products.deduct(change);store.jdbc().update("INSERT INTO order_item(order_id,product_id,product_name,price,quantity) VALUES(?,?,?,?,?)",id,line.productId(),product.name(),product.price(),line.quantity());}
   event(id,"ORDER_CREATED",Map.of("orderId",id,"expiresAt",expires.toString()));return store.detail(store.order(id,false));
  }catch(RuntimeException failure){if(demo)for(var stock:attempted)try{products.restore(stock);}catch(Exception compensation){failure.addSuppressed(compensation);}throw failure;}
 }
 OrderStore.Order own(String id,long uid,boolean lock){var order=store.order(id,lock);if(order.userId()!=uid)throw Errors.fail(404,"订单不存在");return order;}
 public Map<String,Object> detail(String id,long uid){return store.detail(own(id,uid,false));}
 @Transactional
 public Map<String,Object> pay(String id,long uid){var order=own(id,uid,true);if(Set.of("PAID","COMPLETED").contains(order.status()))return store.detail(order);if(!order.status().equals("PENDING"))throw Errors.fail(409,"订单不可支付");if(!order.expiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC)))throw Errors.fail(409,"订单已超时，请取消或等待关单");
  users.debit(new Contracts.Money(id,uid,order.amount()));String pickup=UUID.randomUUID().toString().replace("-","").substring(0,16).toUpperCase(Locale.ROOT);
  store.jdbc().update("UPDATE market_order SET status='PAID',pickup_code=?,paid_at=CURRENT_TIMESTAMP WHERE id=?",pickup,id);
  event(id,"ORDER_PAID",Map.of("orderId",id,"userId",uid,"pickupCode",pickup));return store.detail(store.order(id,false));
 }
 @Transactional
 public Map<String,Object> cancel(String id,long uid,boolean timeoutOnly){var order=uid>0?own(id,uid,true):store.order(id,true);if(order.status().equals("CANCELLED"))return store.detail(order);if(timeoutOnly&&(!order.status().equals("PENDING")||order.expiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))))return store.detail(order);if(!order.status().equals("PENDING"))throw Errors.fail(409,"仅待支付订单可取消");
  // Also closes a possible demo-mode debit whose response was lost before the order update.
  users.refund(new Contracts.Money(id,order.userId(),order.amount()));
  for(var item:store.items(id))products.restore(new Contracts.Stock(id+"_"+item.get("product_id"),((Number)item.get("product_id")).longValue(),((Number)item.get("quantity")).intValue()));
  store.jdbc().update("UPDATE market_order SET status='CANCELLED' WHERE id=?",id);return store.detail(store.order(id,false));
 }
 @Transactional public Map<String,Object> redeem(String code){var rows=store.jdbc().query("SELECT * FROM market_order WHERE pickup_code=? FOR UPDATE",OrderStore.ROW,code);if(rows.isEmpty())throw Errors.fail(404,"提货码不存在");var order=rows.get(0);if(order.status().equals("COMPLETED"))return store.detail(order);if(!order.status().equals("PAID"))throw Errors.fail(409,"订单尚未支付");store.jdbc().update("UPDATE market_order SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE id=?",order.id());return store.detail(store.order(order.id(),false));}
 public List<Map<String,Object>> list(long uid,int page,String status){String where=uid>0?" WHERE user_id=?":" WHERE 1=1";List<Object> params=new ArrayList<>();if(uid>0)params.add(uid);if(status!=null&&!status.isBlank()){if(!Set.of("PENDING","PAID","COMPLETED","CANCELLED").contains(status))throw Errors.fail(400,"订单状态无效");where+=" AND status=?";params.add(status);}params.add((page-1)*20);return store.jdbc().query("SELECT * FROM market_order"+where+" ORDER BY created_at DESC,id LIMIT 20 OFFSET ?",OrderStore.ROW,params.toArray()).stream().map(store::detail).toList();}
}
