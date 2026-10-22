package cn.campus.product;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
@SpringBootApplication @MapperScan(value="cn.campus.product", annotationClass=org.apache.ibatis.annotations.Mapper.class)
public class Application { public static void main(String[] args) {SpringApplication.run(Application.class,args);} }
