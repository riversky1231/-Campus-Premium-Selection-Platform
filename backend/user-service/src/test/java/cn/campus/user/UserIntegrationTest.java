package cn.campus.user;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"market.internal-token=test-internal"}) @AutoConfigureMockMvc
class UserIntegrationTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired WalletService wallet;@Autowired UserStore store;@Autowired AuthService auth;
 AuthService.Credentials student(){return auth.register("u_"+UUID.randomUUID().toString().replace("-","").substring(0,20),"Password123!","Student");}
 @Test void loginRolesAndLogout()throws Exception{var u=student();String token="Bearer "+u.token();mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());mvc.perform(get("/api/users/me").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT"));mvc.perform(get("/api/admin/users").header("Authorization",token)).andExpect(status().isForbidden());mvc.perform(post("/api/auth/logout").header("Authorization",token)).andExpect(status().isOk());mvc.perform(get("/api/users/me").header("Authorization",token)).andExpect(status().isUnauthorized());}
 @Test void addressOwnership()throws Exception{var first=student();var second=student();var r=mvc.perform(post("/api/users/addresses").header("Authorization","Bearer "+first.token()).contentType("application/json").content("{\"recipient\":\"Student\",\"phone\":\"13800138000\",\"detail\":\"Campus A\"}")).andExpect(status().isOk()).andReturn();long id=json.readTree(r.getResponse().getContentAsString()).get("id").asLong();mvc.perform(delete("/api/users/addresses/"+id).header("Authorization","Bearer "+second.token())).andExpect(status().isNotFound());}
 @Test void walletIdempotencyAndInsufficientFunds(){var u=student();long id=((Number)u.user().get("id")).longValue();String request=UUID.randomUUID().toString();var credit=new WalletService.Change(request,id,new BigDecimal("100.00"));wallet.change(credit,"CREDIT");wallet.change(credit,"CREDIT");var debit=new WalletService.Change("d_"+request,id,new BigDecimal("30.00"));wallet.change(debit,"DEBIT");wallet.change(debit,"DEBIT");assertEquals(0,store.id(id).balance().compareTo(new BigDecimal("70.00")));assertThrows(Exception.class,()->wallet.change(new WalletService.Change("x_"+request,id,new BigDecimal("71.00")),"DEBIT"));wallet.change(debit,"REFUND");wallet.change(debit,"REFUND");assertEquals(0,store.id(id).balance().compareTo(new BigDecimal("100.00")));}
 @Test void walletInternalAuthAndRegistrationValidation()throws Exception{mvc.perform(post("/api/internal/wallet/debit").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"username\":\"abc\",\"password\":\"short\",\"nickname\":\"N\"}")).andExpect(status().isBadRequest());}
}
