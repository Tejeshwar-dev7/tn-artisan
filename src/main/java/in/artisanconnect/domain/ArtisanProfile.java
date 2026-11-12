package in.artisanconnect.domain;

import jakarta.persistence.*;

@Entity @Table(name="artisan_profiles")
public class ArtisanProfile {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @OneToOne(optional=false) public User user;
 @Column(nullable=false,length=120) public String shopName;
 @Column(length=80) public String craft;
 @Column(length=120) public String district;
 @Column(length=600) public String story;
 @Column(length=40) public String phone;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public Verification status=Verification.PENDING;
 @Column(length=300) public String verificationNote;
 public Long getId(){return id;} public User getUser(){return user;} public String getShopName(){return shopName;} public String getCraft(){return craft;} public String getDistrict(){return district;} public String getStory(){return story;} public String getPhone(){return phone;} public Verification getStatus(){return status;} public String getVerificationNote(){return verificationNote;}
 public enum Verification { PENDING, APPROVED, REJECTED }
}
