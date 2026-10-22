package cn.campus.product;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
@RestController
public class CatalogController {
 final CatalogService service;
 public CatalogController(CatalogService s){service=s;}
 @GetMapping("/api/categories") public List<Category> categories(){return service.listCategories();}
 @GetMapping({"/api/products","/api/admin/products"}) public Map<String,Object> list(@RequestParam(defaultValue="1") @Min(1) @Max(1000000) int page,@RequestParam(defaultValue="12") @Min(1) @Max(100) int size,@RequestParam(required=false) Long categoryId,@RequestParam(defaultValue="") @Size(max=100) String keyword,jakarta.servlet.http.HttpServletRequest req){
  return service.listProducts(page,size,categoryId,keyword,req.getRequestURI().startsWith("/api/admin/"));
 }
 @GetMapping("/api/internal/products/{id}") public Product internal(@PathVariable long id){return service.require(id);}
 @GetMapping("/api/products/{id}") public Product detail(@PathVariable long id){return service.detail(id);}
 @PostMapping("/api/admin/products") public Product create(@Valid @RequestBody Inputs.NewProduct in){return service.create(in);}
 @PutMapping("/api/admin/products/{id}") public Product update(@PathVariable long id,@Valid @RequestBody Inputs.ProductInput in){return service.update(id,in);}
 @PostMapping("/api/admin/categories") public Category createCategory(@Valid @RequestBody Inputs.CategoryInput in){return service.createCategory(in);}
 @PutMapping("/api/admin/categories/{id}") public Category renameCategory(@PathVariable long id,@Valid @RequestBody Inputs.CategoryInput in){return service.renameCategory(id,in);}
 @DeleteMapping("/api/admin/categories/{id}") public Map<String,Object> deleteCategory(@PathVariable long id){service.deleteCategory(id);return Map.of("deleted",id);}
 @PostMapping("/api/admin/stock/add") public Map<String,Object> add(@Valid @RequestBody Inputs.StockInput in){return service.stock(in,"add");}
 @PostMapping("/api/internal/stock/deduct") public Map<String,Object> deduct(@Valid @RequestBody Inputs.StockInput in){return service.stock(in,"deduct");}
 @PostMapping("/api/internal/stock/restore") public Map<String,Object> restore(@Valid @RequestBody Inputs.StockInput in){return service.stock(in,"restore");}
}
