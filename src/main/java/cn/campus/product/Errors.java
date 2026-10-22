package cn.campus.product;
import java.util.Map;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
@RestControllerAdvice
public class Errors {
 private static final Logger log=LoggerFactory.getLogger(Errors.class);
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> business(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"请求失败":e.getReason()));}
 @ExceptionHandler({MethodArgumentNotValidException.class,org.springframework.web.method.annotation.HandlerMethodValidationException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class}) ResponseEntity<?> bad(Exception e){return ResponseEntity.badRequest().body(Map.of("message","参数格式或范围不正确"));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(Exception e){return ResponseEntity.status(409).body(Map.of("message","名称重复、数据被引用或操作冲突，请刷新后重试"));}
 @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception e){log.error("Request failed",e);return ResponseEntity.status(500).body(Map.of("message","服务暂时不可用"));}
 static ResponseStatusException fail(int code,String text){return new ResponseStatusException(HttpStatus.valueOf(code),text);}
}
