package cn.campus.product;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.*;
import org.slf4j.*;
@Service
public class CatalogService {
 private final ProductMapper products; private final CategoryMapper categories; private final StockMapper operations; private final StringRedisTemplate redis; private final ObjectMapper json;
 @Value("${market.cache-enabled:false}") boolean cache;
 private static final Logger log=LoggerFactory.getLogger(CatalogService.class);
 public CatalogService(ProductMapper p,CategoryMapper c,StockMapper o,StringRedisTemplate r,ObjectMapper j){products=p;categories=c;operations=o;redis=r;json=j;}
 public List<Category> listCategories(){return categories.selectList(new QueryWrapper<Category>().orderByAsc("id"));}
 public Map<String,Object> listProducts(int page,int size,Long categoryId,String keyword,boolean admin){
  QueryWrapper<Product> q=new QueryWrapper<>();
  if(!admin)q.eq("on_sale",true);
  if(categoryId!=null)q.eq("category_id",categoryId);
  if(!keyword.isBlank())q.like("name",keyword);
  long total=products.selectCount(q);
  q.orderByDesc("id").last("LIMIT "+size+" OFFSET "+((long)(page-1)*size));
  return Map.of("items",products.selectList(q),"total",total,"page",page,"size",size);
 }
 @Transactional public Category createCategory(Inputs.CategoryInput in){Category c=new Category();c.setName(in.name().trim());categories.insert(c);return c;}
 @Transactional public Category renameCategory(long id,Inputs.CategoryInput in){Category c=categories.selectById(id);if(c==null)throw Errors.fail(404,"分类不存在");c.setName(in.name().trim());categories.updateById(c);return c;}
 @Transactional public void deleteCategory(long id){if(categories.deleteById(id)==0)throw Errors.fail(404,"分类不存在");}
 public Product require(long id){Product p=products.selectById(id);if(p==null)throw Errors.fail(404,"商品不存在");return p;}
 public Product detail(long id){
  // Stock and sale status always come from the database; cache is descriptive metadata only.
  Product current=require(id);if(!current.getOnSale())throw Errors.fail(404,"商品已下架");
  if(cache)try{String key="product:meta:"+id;String value=redis.opsForValue().get(key);if(value==null)redis.opsForValue().set(key,json.writeValueAsString(current),Duration.ofSeconds(30));else{Product old=json.readValue(value,Product.class);old.setPrice(current.getPrice());old.setStock(current.getStock());old.setOnSale(current.getOnSale());return old;}}catch(Exception e){log.warn("Product cache unavailable; using database");}
  return current;
 }
 void category(long id){if(categories.selectById(id)==null)throw Errors.fail(400,"分类不存在");}
 void invalidate(long id){if(cache)try{redis.delete("product:meta:"+id);}catch(Exception e){log.warn("Cache invalidation failed; metadata expires in 30 seconds");}}
 @Transactional public Product create(Inputs.NewProduct in){category(in.categoryId());Product p=new Product();p.setCategoryId(in.categoryId());p.setName(in.name().trim());p.setDescription(in.description());p.setPrice(in.price());p.setStock(in.stock());p.setOnSale(in.onSale());products.insert(p);return p;}
 @Transactional public Product update(long id,Inputs.ProductInput in){category(in.categoryId());Product p=products.lock(id);if(p==null)throw Errors.fail(404,"商品不存在");p.setCategoryId(in.categoryId());p.setName(in.name().trim());p.setDescription(in.description());p.setPrice(in.price());p.setOnSale(in.onSale());products.updateById(p);invalidate(id);return p;}
 @Transactional public Map<String,Object> stock(Inputs.StockInput in,String action){
  Product p=products.lock(in.productId());if(p==null)throw Errors.fail(404,"商品不存在");
  StockMapper.Operation old=operations.find(in.requestId());
  if(old!=null && (!old.productId().equals(in.productId())||old.quantity()!=in.quantity()))throw Errors.fail(409,"请求号已用于其他库存操作");
  if(action.equals("deduct")){
   if(old!=null){if(!old.state().equals("DEDUCTED"))throw Errors.fail(409,"操作已取消或请求号被占用");}
   else{if(products.deduct(in.productId(),in.quantity())!=1)throw Errors.fail(409,"库存不足或商品已下架");operations.insert(in.requestId(),in.productId(),in.quantity(),"DEDUCTED");}
  }else if(action.equals("restore")){
   if(old==null)operations.insert(in.requestId(),in.productId(),in.quantity(),"CANCELLED");
   else if(old.state().equals("DEDUCTED")){if(products.restore(in.productId(),in.quantity())!=1)throw Errors.fail(409,"库存超出上限");operations.restored(in.requestId());}
   else if(!Set.of("RESTORED","CANCELLED").contains(old.state()))throw Errors.fail(409,"请求号被占用");
  }else{
   if(old!=null){if(!old.state().equals("ADDED"))throw Errors.fail(409,"请求号被占用");}
   else{if(products.restore(in.productId(),in.quantity())!=1)throw Errors.fail(409,"库存超出上限");operations.insert(in.requestId(),in.productId(),in.quantity(),"ADDED");}
  }
  return Map.of("requestId",in.requestId(),"stock",require(in.productId()).getStock(),"state",operations.find(in.requestId()).state());
 }
}
