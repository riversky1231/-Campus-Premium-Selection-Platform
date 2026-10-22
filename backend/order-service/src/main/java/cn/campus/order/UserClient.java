package cn.campus.order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@FeignClient(name="user-service",url="${market.user-url:}")
public interface UserClient extends UserPort {
 @GetMapping("/api/users/identity") Contracts.Identity identity(@RequestHeader("Authorization") String token);
 @GetMapping("/api/internal/users/{uid}/addresses/{id}") Contracts.Address address(@PathVariable("uid") long uid,@PathVariable("id") long id);
 @PostMapping("/api/internal/wallet/debit") Map<String,Object> debit(@RequestBody Contracts.Money in);
 @PostMapping("/api/internal/wallet/refund") Map<String,Object> refund(@RequestBody Contracts.Money in);
}
