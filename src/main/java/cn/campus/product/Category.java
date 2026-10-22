package cn.campus.product;
import com.baomidou.mybatisplus.annotation.*;
public class Category {
@TableId(type=IdType.AUTO)
private Long id;
public Long getId(){return id;}
public void setId(Long value){this.id=value;}
private String name;
public String getName(){return name;}
public void setName(String value){this.name=value;}
}
