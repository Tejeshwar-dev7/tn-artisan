package in.artisanconnect.domain;
import jakarta.persistence.*;
@Entity @Table(name="categories") public class Category { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false,unique=true) public String name; public String district; public Category(){} public Category(String n,String d){name=n;district=d;} public Long getId(){return id;} public String getName(){return name;} public String getDistrict(){return district;} }
