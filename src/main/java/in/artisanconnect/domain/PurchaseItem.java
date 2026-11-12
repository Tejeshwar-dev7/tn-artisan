package in.artisanconnect.domain;
import jakarta.persistence.*; import java.math.BigDecimal;
@Entity @Table(name="order_items") public class PurchaseItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public PurchaseOrder order;
 @ManyToOne(optional=false) public Product product;
 @ManyToOne(optional=false) public User artisan;
 @Column(nullable=false) public int quantity;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal unitPrice;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal commission;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal artisanEarnings;
 public Long getId(){return id;} public PurchaseOrder getOrder(){return order;} public Product getProduct(){return product;} public User getArtisan(){return artisan;} public int getQuantity(){return quantity;} public BigDecimal getUnitPrice(){return unitPrice;} public BigDecimal getLineTotal(){return unitPrice.multiply(BigDecimal.valueOf(quantity));} public BigDecimal getCommission(){return commission;} public BigDecimal getArtisanEarnings(){return artisanEarnings;}
}
