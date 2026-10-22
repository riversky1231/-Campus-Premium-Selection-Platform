package cn.campus.product;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"market.admin-token=test-admin","market.internal-token=test-internal"})
@AutoConfigureMockMvc
class ProductIntegrationTest {
 @Autowired MockMvc mvc; @Autowired CatalogService service;
 long product(int stock){return service.create(new Inputs.NewProduct(1L,"test-"+UUID.randomUUID(),"test",new java.math.BigDecimal("3.50"),stock,true)).getId();}
 @Test void catalogAndSecurity()throws Exception{
  mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
  mvc.perform(get("/api/products?size=101")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/products").header("Authorization","Bearer test-admin")).andExpect(status().isOk());
  mvc.perform(post("/api/internal/stock/deduct").header("Authorization","Bearer test-admin").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/admin/products").header("Authorization","Bearer test-admin").contentType(MediaType.APPLICATION_JSON).content("{\"categoryId\":1,\"name\":\"bad\",\"description\":\"\",\"price\":-1,\"stock\":0,\"onSale\":true}")).andExpect(status().isBadRequest());
 }
 @Test void deductRestoreAndCancellation(){long id=product(10);var in=new Inputs.StockInput(UUID.randomUUID().toString(),id,4);service.stock(in,"deduct");service.stock(in,"deduct");assertEquals(6,service.require(id).getStock());service.stock(in,"restore");service.stock(in,"restore");assertEquals(10,service.require(id).getStock());assertThrows(Exception.class,()->service.stock(in,"deduct"));var cancel=new Inputs.StockInput(UUID.randomUUID().toString(),id,2);service.stock(cancel,"restore");assertThrows(Exception.class,()->service.stock(cancel,"deduct"));assertEquals(10,service.require(id).getStock());}
 @Test void insufficientStockAndPayloadMismatch(){long id=product(2);assertThrows(Exception.class,()->service.stock(new Inputs.StockInput(UUID.randomUUID().toString(),id,3),"deduct"));assertEquals(2,service.require(id).getStock());String key=UUID.randomUUID().toString();service.stock(new Inputs.StockInput(key,id,1),"deduct");assertThrows(Exception.class,()->service.stock(new Inputs.StockInput(key,id,2),"deduct"));assertEquals(1,service.require(id).getStock());}
 @Test void concurrentRequestsNeverOversell()throws Exception{long id=product(5);ExecutorService pool=Executors.newFixedThreadPool(8);try{List<Callable<Boolean>> tasks=new ArrayList<>();for(int i=0;i<16;i++)tasks.add(()->{try{service.stock(new Inputs.StockInput(UUID.randomUUID().toString(),id,1),"deduct");return true;}catch(org.springframework.web.server.ResponseStatusException e){return false;}});long success=0;for(Future<Boolean> f:pool.invokeAll(tasks))if(f.get())success++;assertEquals(5,success);assertEquals(0,service.require(id).getStock());}finally{pool.shutdownNow();}}
 @Test void offSaleAndRestock(){long id=product(2);service.update(id,new Inputs.ProductInput(1L,"hidden","",new java.math.BigDecimal("2.50"),false));assertThrows(Exception.class,()->service.detail(id));assertThrows(Exception.class,()->service.stock(new Inputs.StockInput(UUID.randomUUID().toString(),id,1),"deduct"));var add=new Inputs.StockInput(UUID.randomUUID().toString(),id,3);service.stock(add,"add");service.stock(add,"add");assertEquals(5,service.require(id).getStock());}
 @Test void referencedCategoryCannotBeDeleted()throws Exception{mvc.perform(delete("/api/admin/categories/1").header("Authorization","Bearer test-admin")).andExpect(status().isConflict());}
 @Test void categoryCrudThroughTransactionalProxy()throws Exception{
  String name="cat-"+UUID.randomUUID().toString().substring(0,12);
  var result=mvc.perform(post("/api/admin/categories").header("Authorization","Bearer test-admin").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+name+"\"}")).andExpect(status().isOk()).andReturn();
  long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString()).get("id").asLong();
  mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[*].id",org.hamcrest.Matchers.hasItem((int)id)));
  mvc.perform(put("/api/admin/categories/"+id).header("Authorization","Bearer test-admin").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+name+"-renamed\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value(name+"-renamed"));
  mvc.perform(delete("/api/admin/categories/"+id).header("Authorization","Bearer test-admin")).andExpect(status().isOk());
 }
}
