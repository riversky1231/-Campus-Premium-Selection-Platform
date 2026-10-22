package cn.campus.order;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.*;
import com.alibaba.csp.sentinel.slots.block.degrade.*;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import java.util.List;
@Configuration
public class TrafficRules {
 @PostConstruct void init(){FlowRule r=new FlowRule("/api/orders");r.setGrade(RuleConstant.FLOW_GRADE_QPS);r.setCount(30);FlowRuleManager.loadRules(List.of(r));DegradeRule d=new DegradeRule("/api/orders");d.setGrade(RuleConstant.DEGRADE_GRADE_RT);d.setCount(1500);d.setTimeWindow(10);d.setMinRequestAmount(10);d.setStatIntervalMs(10000);d.setSlowRatioThreshold(0.8);DegradeRuleManager.loadRules(List.of(d));}
}
