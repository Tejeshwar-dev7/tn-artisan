package in.artisanconnect.domain;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.*;
@Entity @Table(name="orders") public class PurchaseOrder {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public User customer;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal total;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) public Status status=Status.ORDER_PLACED;
 @Column(nullable=false) public String shippingAddress;
 @Column(nullable=false,updatable=false) public Instant createdAt=Instant.now();
 @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true) public List<PurchaseItem> items=new ArrayList<>();
 @Column(nullable=false,length=24) public String paymentMethod="LEGACY_DEMO";
 @Column(nullable=false,length=24) public String paymentStatus="PENDING";
 @Column(length=80) public String providerOrderId;
 @Column(length=80) public String paymentReference;
 @Column(length=120) public String recipientName;
 @Column(length=32) public String deliveryPhone;
 @Column(length=180) public String addressLine1;
 @Column(length=180) public String addressLine2;
 @Column(length=100) public String deliveryCity;
 @Column(length=100) public String deliveryDistrict;
 @Column(length=100) public String deliveryState;
 @Column(length=12) public String deliveryPostalCode;
 @Column(length=100) public String courierName;
 @Column(length=100) public String trackingNumber;
 @Column(length=500) public String externalTrackingUrl;
 @Column(length=180) public String latestShipmentLocation;
 public Instant expectedDeliveryAt;
 public Instant dispatchedAt;
 public Instant deliveredAt;
 public String getPaymentMethod(){return paymentMethod;}
 public String getPaymentStatus(){return paymentStatus;} public String getPaymentReference(){return paymentReference;} public String getProviderOrderId(){return providerOrderId;}
 public String getRecipientName(){return recipientName;} public String getDeliveryPhone(){return deliveryPhone;} public String getAddressLine1(){return addressLine1;} public String getAddressLine2(){return addressLine2;} public String getDeliveryCity(){return deliveryCity;} public String getDeliveryDistrict(){return deliveryDistrict;} public String getDeliveryState(){return deliveryState;} public String getDeliveryPostalCode(){return deliveryPostalCode;}
 public String getCourierName(){return courierName;} public String getTrackingNumber(){return trackingNumber;} public String getExternalTrackingUrl(){return externalTrackingUrl;} public String getLatestShipmentLocation(){return latestShipmentLocation;} public Instant getExpectedDeliveryAt(){return expectedDeliveryAt;} public Instant getDispatchedAt(){return dispatchedAt;} public Instant getDeliveredAt(){return deliveredAt;}
 public Long getId(){return id;} public User getCustomer(){return customer;} public BigDecimal getTotal(){return total;} public Status getStatus(){return status;} public String getShippingAddress(){return shippingAddress;} public Instant getCreatedAt(){return createdAt;} public List<PurchaseItem> getItems(){return items;}
 public enum Status { PAYMENT_PENDING, PAYMENT_CONFIRMED, PAYMENT_FAILED, ORDER_PLACED, ACCEPTED, PRODUCTION_STARTED, QUALITY_CHECK, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, REFUND_REQUESTED, REFUNDED }
}
