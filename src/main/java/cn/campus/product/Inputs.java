package cn.campus.product;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public final class Inputs {
 public record ProductInput(@NotNull @Positive Long categoryId,@NotBlank @Size(max=100) String name,@NotNull @Size(max=1000) String description,@NotNull @DecimalMin("0.01") @DecimalMax("99999999.99") @Digits(integer=8,fraction=2) BigDecimal price,@NotNull Boolean onSale) {}
 public record NewProduct(@NotNull @Positive Long categoryId,@NotBlank @Size(max=100) String name,@NotNull @Size(max=1000) String description,@NotNull @DecimalMin("0.01") @DecimalMax("99999999.99") @Digits(integer=8,fraction=2) BigDecimal price,@Min(0) int stock,@NotNull Boolean onSale) {}
 public record CategoryInput(@NotBlank @Size(max=40) String name) {}
 public record StockInput(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,80}") String requestId,@Positive long productId,@Min(1) @Max(1000000) int quantity) {}

 public record CartInput(@Min(0) @Max(99) int quantity) {}
}
