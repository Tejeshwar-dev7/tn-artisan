package in.artisanconnect.domain;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.Instant;
@Entity @Table(name="earnings_ledger",uniqueConstraints=@UniqueConstraint(columnNames="orderItemId"))
public class EarningsLedger{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true) public Long orderItemId;
 @ManyToOne(optional=false) public User artisan;
 @ManyToOne(optional=false) public PurchaseOrder order;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal grossAmount;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal platformCommission;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal artisanAmount;
 @Column(nullable=false,length=24) public String payoutStatus="PENDING";
 @Column(nullable=false,updatable=false) public Instant recordedAt=Instant.now();
}
