package cn.campus.monitor;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
@SpringBootApplication @EnableAdminServer public class MonitorApplication {public static void main(String[] a){SpringApplication.run(MonitorApplication.class,a);}}
