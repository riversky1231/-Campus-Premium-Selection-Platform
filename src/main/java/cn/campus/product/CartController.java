package cn.campus.product;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.*;
@RestController
public class CartController {
 private final CartStorage carts;private final CatalogService catalog;private final IdentityService identities;
 public CartController(CartStorage c,CatalogService s,IdentityService i){carts=c;catalog=s;identities=i;}
 String key(String token){return "cart:"+identities.identity(token).userId();}
 @GetMapping("/api/cart") public List<Map<String,Object>> list(@RequestHeader(value="Authorization",required=false) String token){List<Map<String,Object>> result=new ArrayList<>();carts.list(key(token)).forEach((id,quantity)->result.add(Map.of("product",catalog.require(Long.parseLong(id)),"quantity",quantity)));return result;}
 @PutMapping("/api/cart/{id}") public Map<String,Object> put(@PathVariable long id,@RequestHeader(value="Authorization",required=false) String token,@Valid @RequestBody Inputs.CartInput input){String key=key(token);if(input.quantity()>0){Product product=catalog.detail(id);if(input.quantity()>product.getStock())throw Errors.fail(409,"库存不足");}carts.put(key,id,input.quantity());return Map.of("productId",id,"quantity",input.quantity());}
 @DeleteMapping("/api/cart") public Map<String,Object> clear(@RequestHeader(value="Authorization",required=false) String token){String key=key(token);carts.list(key).keySet().forEach(id->carts.put(key,Long.parseLong(id),0));return Map.of("success",true);}
}
