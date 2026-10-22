package cn.campus.user;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;
import java.util.List;
@Configuration @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
public class SecurityConfig {
 @Bean org.springframework.security.core.userdetails.UserDetailsService unusedDefaultUser(){return username->{throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use bearer login");};}
 @Bean PasswordEncoder passwords(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain chain(HttpSecurity http,AuthService auth,@Value("${market.internal-token:}") String internal)throws Exception{
  http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
  http.authorizeHttpRequests(a->a.requestMatchers("/api/auth/login","/api/auth/register","/api/auth/wx-login","/actuator/health","/actuator/info","/error").permitAll().requestMatchers("/api/internal/**").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated());
  http.exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->error(s,401,"请先登录")).accessDeniedHandler((r,s,x)->error(s,403,"权限不足")));
  http.addFilterBefore(new OncePerRequestFilter(){protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
   String path=UriUtils.decode(req.getRequestURI(),StandardCharsets.UTF_8);String token=req.getHeader("Authorization");
   if(path.startsWith("/api/internal/")){if(internal.isBlank()||token==null||!MessageDigest.isEqual(("Bearer "+internal).getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8))){error(res,401,"内部凭证无效");return;}}
   else if(token!=null){try{var u=auth.authenticate(token);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,List.of(new SimpleGrantedAuthority("ROLE_"+u.role()))));}catch(org.springframework.web.server.ResponseStatusException e){error(res,e.getStatusCode().value(),e.getReason());return;}}
   chain.doFilter(req,res);
  }},UsernamePasswordAuthenticationFilter.class);return http.build();
 }
 static void error(HttpServletResponse res,int status,String message)throws IOException{res.setStatus(status);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\""+message+"\"}");}
 public static UserStore.User current(){return (UserStore.User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();}
}
