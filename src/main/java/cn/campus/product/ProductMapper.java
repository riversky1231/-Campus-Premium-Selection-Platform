package cn.campus.product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
@org.apache.ibatis.annotations.Mapper
public interface ProductMapper extends BaseMapper<Product> {
 @Select("SELECT * FROM product WHERE id=#{id} FOR UPDATE") Product lock(long id);
 @Update("UPDATE product SET stock=stock-#{quantity} WHERE id=#{id} AND on_sale=true AND stock>=#{quantity}") int deduct(@Param("id") long id,@Param("quantity") int quantity);
 @Update("UPDATE product SET stock=stock+#{quantity} WHERE id=#{id} AND stock<=2147483647-#{quantity}") int restore(@Param("id") long id,@Param("quantity") int quantity);
}
