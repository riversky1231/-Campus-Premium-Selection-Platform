package cn.campus.product;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;
@Component
public class Security extends OncePerRequestFilter {
 @Value("${market.admin-token:}") String admin;
 @Value("${market.internal-token:}") String internal;
 private final IdentityService identities;
 public Security(IdentityService i){identities=i;}
 boolean match(String expected,String got){return !expected.isBlank()&&got!=null&&MessageDigest.isEqual(("Bearer "+expected).getBytes(StandardCharsets.UTF_8),got.getBytes(StandardCharsets.UTF_8));}
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
  String path=UriUtils.decode(req.getRequestURI(),StandardCharsets.UTF_8),token=req.getHeader("Authorization");
  try{
   if(path.startsWith("/api/internal/")&&!match(internal,token))throw Errors.fail(401,"内部凭证无效");
   if(path.startsWith("/api/admin/")&&!match(admin,token)){
    var user=identities.identity(token);if(!"ADMIN".equals(user.role()))throw Errors.fail(403,"需要管理员权限");
   }
  }catch(org.springframework.web.server.ResponseStatusException e){res.setStatus(e.getStatusCode().value());res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\""+e.getReason()+"\"}");return;}
  chain.doFilter(req,res);
 }
}
