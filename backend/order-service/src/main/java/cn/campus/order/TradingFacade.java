package cn.campus.order;
import org.springframework.stereotype.Service;
import io.seata.spring.annotation.GlobalTransactional;
import java.util.Map;
@Service
public class TradingFacade {
 private final OrderService service;
 public TradingFacade(OrderService s){service=s;}
 @GlobalTransactional(name="campus-create",rollbackFor=Exception.class)
 public Map<String,Object> create(long uid,OrderService.Create in){return service.create(uid,in);}
 @GlobalTransactional(name="campus-pay",rollbackFor=Exception.class)
 public Map<String,Object> pay(String id,long uid){return service.pay(id,uid);}
 @GlobalTransactional(name="campus-cancel",rollbackFor=Exception.class)
 public Map<String,Object> cancel(String id,long uid,boolean expiry){return service.cancel(id,uid,expiry);}
}
