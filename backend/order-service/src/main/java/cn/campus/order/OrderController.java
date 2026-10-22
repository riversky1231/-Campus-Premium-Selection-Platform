package cn.campus.order;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
@RestController
public class OrderController {
 private final OrderService service;private final TradingFacade trading;private final OrderStore store;
 public OrderController(OrderService s,OrderStore db,TradingFacade t){service=s;store=db;trading=t;}
 long uid(Contracts.Identity identity){return Long.parseLong(identity.userId());}
 @PostMapping("/api/orders") public Map<String,Object> create(@RequestAttribute("identity") Contracts.Identity identity,@Valid @RequestBody OrderService.Create in){return trading.create(uid(identity),in);}
 @GetMapping("/api/orders") public List<Map<String,Object>> list(@RequestAttribute("identity") Contracts.Identity identity,@RequestParam(defaultValue="1") @Min(1) @Max(1000000) int page,@RequestParam(required=false) String status){return service.list(uid(identity),page,status);}
 @GetMapping("/api/orders/{id}") public Map<String,Object> detail(@RequestAttribute("identity") Contracts.Identity identity,@PathVariable String id){return service.detail(id,uid(identity));}
 @PostMapping("/api/orders/{id}/pay") public Map<String,Object> pay(@RequestAttribute("identity") Contracts.Identity identity,@PathVariable String id){return trading.pay(id,uid(identity));}
 @PostMapping("/api/orders/{id}/cancel") public Map<String,Object> cancel(@RequestAttribute("identity") Contracts.Identity identity,@PathVariable String id){return trading.cancel(id,uid(identity),false);}
 @GetMapping("/api/admin/orders") public List<Map<String,Object>> admin(@RequestParam(defaultValue="1") @Min(1) @Max(1000000) int page,@RequestParam(required=false) String status){return service.list(0,page,status);}
 public record Redeem(@NotBlank @Pattern(regexp="[A-F0-9]{16}") String pickupCode){}
 @PostMapping("/api/admin/orders/redeem") public Map<String,Object> redeem(@Valid @RequestBody Redeem in){return service.redeem(in.pickupCode());}
 @GetMapping("/api/notices") public List<Map<String,Object>> notices(@RequestAttribute("identity") Contracts.Identity identity,@RequestParam(defaultValue="1") @Min(1) @Max(1000000) int page){return store.rows("SELECT event_id,order_id,message,read_flag,created_at FROM user_notice WHERE user_id=? ORDER BY created_at DESC,event_id LIMIT 50 OFFSET ?",uid(identity),(page-1)*50);}
 @PostMapping("/api/notices/{id}/read") public Map<String,Object> read(@PathVariable String id,@RequestAttribute("identity") Contracts.Identity identity){if(store.jdbc().update("UPDATE user_notice SET read_flag=true WHERE event_id=? AND user_id=?",id,uid(identity))==0)throw Errors.fail(404,"通知不存在");return Map.of("success",true);}
}
