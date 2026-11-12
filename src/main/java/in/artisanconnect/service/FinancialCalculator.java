package in.artisanconnect.service;
import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import java.math.BigDecimal;import java.math.RoundingMode;
@Component public class FinancialCalculator{
 private final BigDecimal commissionRate;
 public FinancialCalculator(@Value("${app.commission-percent:10}") BigDecimal percent){commissionRate=percent.divide(new BigDecimal("100"),6,RoundingMode.HALF_UP);}
 public BigDecimal commission(BigDecimal lineAmount){return lineAmount.multiply(commissionRate).setScale(2,RoundingMode.HALF_UP);}
 public BigDecimal artisanShare(BigDecimal lineAmount){return lineAmount.subtract(commission(lineAmount)).setScale(2,RoundingMode.HALF_UP);}
}
