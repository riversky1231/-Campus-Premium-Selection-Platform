package cn.campus.user;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.*;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import java.util.List;
@Configuration
public class TrafficRules {
 @PostConstruct void init(){FlowRule r=new FlowRule("/api/auth/login");r.setGrade(RuleConstant.FLOW_GRADE_QPS);r.setCount(10);FlowRuleManager.loadRules(List.of(r));}
}
