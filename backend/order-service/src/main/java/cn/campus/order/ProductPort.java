package cn.campus.order;
import java.util.Map;
public interface ProductPort {
 Contracts.Product product(long id);
 Map<String,Object> deduct(Contracts.Stock in);
 Map<String,Object> restore(Contracts.Stock in);
}
