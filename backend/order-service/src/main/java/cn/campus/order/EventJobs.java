package cn.campus.order;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.*;
import java.time.*;
@Component
public class EventJobs {
 private final OutboxPublisher publisher;private final OrderStore store;private final TradingFacade orders;
 private static final Logger log=LoggerFactory.getLogger(EventJobs.class);
 public EventJobs(OutboxPublisher p,OrderStore s,TradingFacade o){publisher=p;store=s;orders=o;}
 @Scheduled(fixedDelayString="${market.outbox-poll-ms:3000}") public void events(){try{for(int i=0;i<20&&publisher.dispatchNext();i++){} }catch(Exception e){log.warn("Outbox delivery failed; pending row retained for retry: {}",e.getClass().getSimpleName());}}
 @Scheduled(fixedDelayString="${market.expiry-poll-ms:30000}") public void expiry(){var ids=store.jdbc().queryForList("SELECT id FROM market_order WHERE status='PENDING' AND expires_at<=? ORDER BY expires_at LIMIT 100",String.class,java.sql.Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)));for(String id:ids)try{orders.cancel(id,0,true);}catch(Exception e){log.warn("Order expiry will retry: {}",id);}}
}
