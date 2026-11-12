package in.artisanconnect.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="users", uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=120) public String name;
 @Column(nullable=false,length=190) public String email;
 @Column(nullable=false) public String passwordHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) public Role role=Role.CUSTOMER;
 @Column(nullable=false) public boolean active=true;
 @Column(nullable=false,updatable=false) public Instant createdAt=Instant.now();
 @Column(length=32) public String phone;
 @Column(length=180) public String addressLine1;
 @Column(length=180) public String addressLine2;
 @Column(length=100) public String city;
 @Column(length=100) public String district;
 @Column(length=100) public String state;
 @Column(length=12) public String postalCode;
 public User(){}
 public User(String name,String email,String passwordHash,Role role){this.name=name;this.email=email.toLowerCase();this.passwordHash=passwordHash;this.role=role;}
 public Long getId(){return id;} public String getName(){return name;} public String getEmail(){return email;} public Role getRole(){return role;} public boolean isActive(){return active;} public Instant getCreatedAt(){return createdAt;} public String getPhone(){return phone;} public String getAddressLine1(){return addressLine1;} public String getAddressLine2(){return addressLine2;} public String getCity(){return city;} public String getDistrict(){return district;} public String getState(){return state;} public String getPostalCode(){return postalCode;}
 public enum Role { CUSTOMER, ARTISAN, COORDINATOR, ADMIN }
}
