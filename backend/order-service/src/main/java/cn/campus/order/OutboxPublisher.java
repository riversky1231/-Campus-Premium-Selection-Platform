package cn.campus.order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.spring.annotation.GlobalLock;
import java.time.*;
import java.util.concurrent.TimeUnit;
@Service
public class OutboxPublisher {
 private final OrderStore store;private final EventConsumer consumer;private final RabbitTemplate rabbit;private final ObjectMapper json;
 @Value("${market.rabbit-enabled:false}") boolean enabled;
 public OutboxPublisher(OrderStore s,EventConsumer c,RabbitTemplate r,ObjectMapper j){store=s;consumer=c;rabbit=r;json=j;}
 @GlobalLock @Transactional(rollbackFor=Exception.class)
 public boolean dispatchNext()throws Exception{
  var rows=store.jdbc().queryForList("SELECT * FROM event_outbox WHERE sent=false ORDER BY created_at,id LIMIT 1 FOR UPDATE");if(rows.isEmpty())return false;var row=rows.get(0);var event=new EventConsumer.Event((String)row.get("id"),(String)row.get("order_id"),(String)row.get("event_type"),(String)row.get("payload"));
  if(enabled){var props=new MessageProperties();props.setContentType("application/json");props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);props.setMessageId(event.eventId());
   if(event.type().equals("ORDER_CREATED")){var order=store.order(event.orderId(),false);long millis=Math.max(1,Duration.between(LocalDateTime.now(ZoneOffset.UTC),order.expiresAt()).toMillis());props.setExpiration(String.valueOf(millis));}
   var correlation=new CorrelationData(event.eventId());rabbit.send("campus.events",event.type().equals("ORDER_CREATED")?"created":"paid",new Message(json.writeValueAsBytes(event),props),correlation);
   var confirmed=correlation.getFuture().get(5,TimeUnit.SECONDS);if(!confirmed.isAck()||correlation.getReturned()!=null)throw new IllegalStateException("Broker did not confirm routing");
  }else if(event.type().equals("ORDER_PAID"))consumer.notice(event);
  store.jdbc().update("UPDATE event_outbox SET sent=true,attempts=attempts+1 WHERE id=?",event.eventId());return true;
 }
}
