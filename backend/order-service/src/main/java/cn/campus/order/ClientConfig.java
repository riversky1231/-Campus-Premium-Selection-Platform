package cn.campus.order;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import feign.*;
@Configuration
public class ClientConfig {
 @Bean RequestInterceptor internal(@Value("${market.internal-token:}") String token){return request->{if(request.path().startsWith("/api/internal/")){if(token.isBlank())throw Errors.fail(503,"内部令牌未配置");request.header("Authorization","Bearer "+token);}String xid=io.seata.core.context.RootContext.getXID();if(xid!=null)request.header("TX_XID",xid);};}
 @Bean Retryer retryer(){return Retryer.NEVER_RETRY;}
 @Bean feign.codec.ErrorDecoder errorDecoder(){return (method,response)->{int status=response.status();String message=switch(status){case 401->"服务凭证无效";case 403->"操作被拒绝";case 404->"关联数据不存在";case 409->"余额或库存不足，或操作状态冲突";default->"关联服务暂不可用";};return Errors.fail(status>=400&&status<500?status:503,message);};}
}
