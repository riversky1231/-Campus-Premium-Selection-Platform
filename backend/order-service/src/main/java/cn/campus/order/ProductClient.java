package cn.campus.order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@FeignClient(name="product-service",url="${market.product-url:}")
public interface ProductClient extends ProductPort {
 @GetMapping("/api/internal/products/{id}") Contracts.Product product(@PathVariable("id") long id);
 @PostMapping("/api/internal/stock/deduct") Map<String,Object> deduct(@RequestBody Contracts.Stock in);
 @PostMapping("/api/internal/stock/restore") Map<String,Object> restore(@RequestBody Contracts.Stock in);
}
