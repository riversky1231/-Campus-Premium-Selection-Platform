package cn.campus.product;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
@Component
public class CartStorage {
 private final StringRedisTemplate redis;
 @Value("${market.cache-enabled:false}") boolean enabled;
 private final Map<String,Map<String,Integer>> memory=new ConcurrentHashMap<>();
 public CartStorage(StringRedisTemplate r){redis=r;}
 public Map<String,Integer> list(String key){if(!enabled)return new TreeMap<>(memory.getOrDefault(key,Map.of()));Map<String,Integer> result=new TreeMap<>();redis.opsForHash().entries(key).forEach((k,v)->result.put(k.toString(),Integer.parseInt(v.toString())));return result;}
 public void put(String key,long id,int quantity){String field=String.valueOf(id);if(!enabled){var items=memory.computeIfAbsent(key,k->new ConcurrentHashMap<>());if(quantity==0)items.remove(field);else items.put(field,quantity);return;}if(quantity==0)redis.opsForHash().delete(key,field);else{redis.opsForHash().put(key,field,String.valueOf(quantity));redis.expire(key,Duration.ofDays(30));}}
}
