package cn.campus.order;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
@Configuration @ConditionalOnProperty(name="market.rabbit-enabled",havingValue="true")
public class RabbitConfig {
 @Bean DirectExchange events(){return new DirectExchange("campus.events",true,false);}
 @Bean DirectExchange dead(){return new DirectExchange("campus.dead",true,false);}
 @Bean Queue delay(){return QueueBuilder.durable("campus.order.delay").deadLetterExchange("campus.events").deadLetterRoutingKey("expire").build();}
 @Bean Queue expiry(){return QueueBuilder.durable("campus.order.expire").deadLetterExchange("campus.dead").deadLetterRoutingKey("failed").build();}
 @Bean Queue notice(){return QueueBuilder.durable("campus.order.notice").deadLetterExchange("campus.dead").deadLetterRoutingKey("failed").build();}
 @Bean Queue failures(){return QueueBuilder.durable("campus.failed").build();}
 @Bean Binding delayBinding(){return BindingBuilder.bind(delay()).to(events()).with("created");}
 @Bean Binding expiryBinding(){return BindingBuilder.bind(expiry()).to(events()).with("expire");}
 @Bean Binding noticeBinding(){return BindingBuilder.bind(notice()).to(events()).with("paid");}
 @Bean Binding failureBinding(){return BindingBuilder.bind(failures()).to(dead()).with("failed");}
 @Component @ConditionalOnProperty(name="market.rabbit-enabled",havingValue="true")
 public static class Listener {
  private final EventConsumer consumer;private final ObjectMapper json;
  public Listener(EventConsumer c,ObjectMapper j){consumer=c;json=j;}
  @RabbitListener(queues={"campus.order.expire","campus.order.notice"}) public void receive(Message message)throws Exception{consumer.accept(json.readValue(message.getBody(),EventConsumer.Event.class));}
 }
}
