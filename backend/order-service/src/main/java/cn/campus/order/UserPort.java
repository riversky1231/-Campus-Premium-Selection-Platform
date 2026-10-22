package cn.campus.order;
import java.util.Map;
public interface UserPort {
 Contracts.Identity identity(String token);
 Contracts.Address address(long uid,long id);
 Map<String,Object> debit(Contracts.Money in);
 Map<String,Object> refund(Contracts.Money in);
}
