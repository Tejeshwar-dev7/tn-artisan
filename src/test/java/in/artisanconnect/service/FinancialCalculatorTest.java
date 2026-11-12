package in.artisanconnect.service;
import org.junit.jupiter.api.Test;import java.math.BigDecimal;import static org.junit.jupiter.api.Assertions.assertEquals;
class FinancialCalculatorTest{
 private final FinancialCalculator calculator=new FinancialCalculator(new BigDecimal("10"));
 @Test void calculatesCommissionAndArtisanShareToPaise(){BigDecimal line=new BigDecimal("1999.95");assertEquals(new BigDecimal("200.00"),calculator.commission(line));assertEquals(new BigDecimal("1799.95"),calculator.artisanShare(line));}
 @Test void roundsHalfUpAtThePaiseBoundary(){assertEquals(new BigDecimal("0.01"),calculator.commission(new BigDecimal("0.05")));}
}
