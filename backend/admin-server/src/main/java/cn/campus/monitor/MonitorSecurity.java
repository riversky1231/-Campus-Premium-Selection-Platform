package cn.campus.monitor;
import org.springframework.context.annotation.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import static org.springframework.security.config.Customizer.withDefaults;
@Configuration public class MonitorSecurity {
 @Bean SecurityFilterChain security(HttpSecurity http)throws Exception{return http.authorizeHttpRequests(a->a.anyRequest().authenticated()).httpBasic(withDefaults()).csrf(c->c.ignoringRequestMatchers("/instances","/instances/**")).build();}
}
