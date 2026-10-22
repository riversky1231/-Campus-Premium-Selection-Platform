package cn.campus.product;
import org.apache.ibatis.annotations.*;
@org.apache.ibatis.annotations.Mapper
public interface StockMapper {
 record Operation(String requestId,Long productId,Integer quantity,String state) {}
 @Select("SELECT request_id,product_id,quantity,state FROM stock_operation WHERE request_id=#{id}") Operation find(String id);
 @Insert("INSERT INTO stock_operation(request_id,product_id,quantity,state) VALUES(#{id},#{productId},#{quantity},#{state})") void insert(@Param("id") String id,@Param("productId") long productId,@Param("quantity") int quantity,@Param("state") String state);
 @Update("UPDATE stock_operation SET state='RESTORED' WHERE request_id=#{id}") void restored(String id);
}
