package in.artisanconnect.domain;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="products") public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=160) public String name;
 @Column(nullable=false,length=3000) public String description;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal price;
 @Column(nullable=false) public int stock;
 @Column(length=160) public String materials;
 @Column(length=100) public String technique;
 @Column(length=80) public String district;
 @Column(length=500) public String imageUrl;
 @Column(length=180) public String dimensions;
 @Column(name="image_credit",length=300) public String imageCredit;
 @Column(name="image_source",length=500) public String imageSource;
 @ManyToOne(optional=false) public User artisan;
 @ManyToOne(optional=false) public Category category;
 @Column(nullable=false) public boolean active=true;
 @Column(nullable=false,updatable=false) public Instant createdAt=Instant.now();
 public Product(){}
 public Long getId(){return id;} public String getName(){return name;} public String getDescription(){return description;} public BigDecimal getPrice(){return price;} public int getStock(){return stock;} public String getMaterials(){return materials;} public String getTechnique(){return technique;} public String getDistrict(){return district;} public String getImageUrl(){return imageUrl;} public String getDimensions(){return dimensions;} public String getImageCredit(){return imageCredit;} public String getImageSource(){return imageSource;} public User getArtisan(){return artisan;} public Category getCategory(){return category;} public boolean isActive(){return active;} public Instant getCreatedAt(){return createdAt;}
}
