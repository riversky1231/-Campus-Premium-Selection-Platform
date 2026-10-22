package cn.campus.order;
import org.junit.jupiter.api.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.datasource.*;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@SpringJUnitConfig(OrderIntegrationTest.Config.class)
class OrderIntegrationTest {
 @Configuration @EnableTransactionManagement @Import({OrderService.class,OrderStore.class})
 static class Config {
  @Bean DataSource dataSource(){var ds=new DriverManagerDataSource("jdbc:h2:mem:order_tests;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");new ResourceDatabasePopulator(new ClassPathResource("db/schema.sql")).execute(ds);return ds;}
  @Bean org.springframework.jdbc.core.JdbcTemplate jdbc(DataSource ds){return new org.springframework.jdbc.core.JdbcTemplate(ds);}
  @Bean DataSourceTransactionManager transactionManager(DataSource ds){return new DataSourceTransactionManager(ds);}
  @Bean ProductPort products(){return mock(ProductPort.class);}
  @Bean UserPort users(){return mock(UserPort.class);}
  @Bean com.fasterxml.jackson.databind.ObjectMapper json(){return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();}
 }
 @Autowired ProductPort products;@Autowired UserPort users;
 @Autowired OrderService service;@Autowired OrderStore store;
 @BeforeEach void setup(){reset(products,users);when(products.product(1)).thenReturn(new Contracts.Product(1L,1L,"Test","",new BigDecimal("10.00"),100,true));when(products.deduct(any())).thenReturn(Map.of("stock",99));when(products.restore(any())).thenReturn(Map.of("stock",100));when(users.address(1,1)).thenReturn(new Contracts.Address(1,"Student","13800138000","Campus A"));when(users.debit(any())).thenReturn(Map.of("balance",90));when(users.refund(any())).thenReturn(Map.of("balance",100));}
 Map<String,Object> create(){return service.create(1,new OrderService.Create(UUID.randomUUID().toString(),1,List.of(new OrderService.Line(1,1))));}
 @Test void repeatSubmissionAndPayment(){var input=new OrderService.Create(UUID.randomUUID().toString(),1,List.of(new OrderService.Line(1,1)));var first=service.create(1,input);assertEquals(first.get("id"),service.create(1,input).get("id"));String id=(String)first.get("id");service.pay(id,1);service.pay(id,1);verify(products,times(1)).deduct(any());verify(users,times(1)).debit(any());}
 @Test void cancelAndOwnership(){String id=(String)create().get("id");assertThrows(Exception.class,()->service.detail(id,2));service.cancel(id,1,false);service.cancel(id,1,false);verify(products,times(1)).restore(any());assertEquals("CANCELLED",service.detail(id,1).get("status"));}
 @Test void expireAndPickup(){String id=(String)create().get("id");store.jdbc().update("UPDATE market_order SET expires_at=? WHERE id=?",java.sql.Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)),id);assertThrows(Exception.class,()->service.pay(id,1));service.cancel(id,0,true);assertEquals("CANCELLED",service.detail(id,1).get("status"));String next=(String)create().get("id");String code=(String)service.pay(next,1).get("pickupCode");assertEquals("COMPLETED",service.redeem(code).get("status"));assertEquals("COMPLETED",service.redeem(code).get("status"));}
 @Test void insufficientBalanceRollsBackOrderState(){String id=(String)create().get("id");when(users.debit(any())).thenThrow(Errors.fail(409,"Insufficient balance"));assertThrows(Exception.class,()->service.pay(id,1));assertEquals("PENDING",service.detail(id,1).get("status"));assertNull(service.detail(id,1).get("pickupCode"));}
}
