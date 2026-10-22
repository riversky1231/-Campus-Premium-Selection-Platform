package cn.campus.order;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Repository
public class OrderStore {
 private final JdbcTemplate db;
 public OrderStore(JdbcTemplate d){db=d;}
 public List<Map<String,Object>> rows(String sql,Object... args){return db.queryForList(sql,args).stream().map(row->{Map<String,Object> result=new LinkedHashMap<>();row.forEach((key,value)->result.put(key.toLowerCase(Locale.ROOT),value));return result;}).toList();}
 public JdbcTemplate jdbc(){return db;}
 public record Order(String id,long userId,String requestId,String fingerprint,String status,BigDecimal amount,String recipient,String phone,String addressDetail,String pickupCode,LocalDateTime createdAt,LocalDateTime expiresAt){}
 static final org.springframework.jdbc.core.RowMapper<Order> ROW=(r,n)->new Order(r.getString("id"),r.getLong("user_id"),r.getString("request_id"),r.getString("fingerprint"),r.getString("status"),r.getBigDecimal("amount"),r.getString("recipient"),r.getString("phone"),r.getString("address_detail"),r.getString("pickup_code"),r.getTimestamp("created_at").toLocalDateTime(),r.getTimestamp("expires_at").toLocalDateTime());
 Order order(String id,boolean lock){return db.query("SELECT * FROM market_order WHERE id=?"+(lock?" FOR UPDATE":""),ROW,id).stream().findFirst().orElseThrow(()->Errors.fail(404,"订单不存在"));}
 List<Map<String,Object>> items(String id){return rows("SELECT product_id,product_name,price,quantity FROM order_item WHERE order_id=? ORDER BY product_id",id);}
 public Map<String,Object> detail(Order o){Map<String,Object> r=new LinkedHashMap<>();r.put("id",o.id());r.put("userId",o.userId());r.put("status",o.status());r.put("amount",o.amount());r.put("recipient",o.recipient());r.put("phone",o.phone());r.put("address",o.addressDetail());r.put("pickupCode",o.pickupCode());r.put("createdAt",o.createdAt());r.put("expiresAt",o.expiresAt());r.put("items",items(o.id()));return r;}
}
