package cn.campus.order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
@Service
public class EventConsumer {
 public record Event(String eventId,String orderId,String type,String payload){}
 private final TradingFacade orders;private final OrderStore store;private final ObjectMapper json;
 public EventConsumer(TradingFacade o,OrderStore s,ObjectMapper j){orders=o;store=s;json=j;}
 public void accept(Event e)throws Exception{
  if("ORDER_CREATED".equals(e.type()))orders.cancel(e.orderId(),0,true);
  else if("ORDER_PAID".equals(e.type()))notice(e);
  else throw new IllegalArgumentException("Unknown event type");
 }
 @Transactional public void notice(Event e)throws Exception{
  var order=store.order(e.orderId(),false);if(!Set.of("PAID","COMPLETED").contains(order.status()))throw new IllegalStateException("Payment not committed");
  // Unique event id makes redelivery harmless; keep duplicate handling outside transaction.
  if(!store.jdbc().queryForList("SELECT event_id FROM user_notice WHERE event_id=?",e.eventId()).isEmpty())return;
  try{store.jdbc().update("INSERT INTO user_notice(event_id,user_id,order_id,message) VALUES(?,?,?,?)",e.eventId(),order.userId(),e.orderId(),"支付成功，请凭提货码 "+order.pickupCode()+" 到自提点取货。");}catch(org.springframework.dao.DuplicateKeyException duplicate){/* another delivery already inserted */}
 }
}
