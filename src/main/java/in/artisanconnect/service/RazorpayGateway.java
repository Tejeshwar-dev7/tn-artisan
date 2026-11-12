package in.artisanconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Server-side Razorpay Orders API and captured-payment verification. Secrets never reach the browser. */
@Service
public class RazorpayGateway {
 private final String keyId, keySecret, webhookSecret;
 private final ObjectMapper json;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(8)).build();

 public RazorpayGateway(@Value("${app.payment.razorpay-key-id:}") String keyId,
                        @Value("${app.payment.razorpay-key-secret:}") String keySecret,
                        @Value("${app.payment.razorpay-webhook-secret:}") String webhookSecret,
                        ObjectMapper json) {
  this.keyId=keyId==null?"":keyId.trim(); this.keySecret=keySecret==null?"":keySecret.trim();
  this.webhookSecret=webhookSecret==null?"":webhookSecret.trim(); this.json=json;
 }
 public boolean configured(){return !keyId.isBlank()&&!keySecret.isBlank();}
 public boolean webhookConfigured(){return !webhookSecret.isBlank();}
 public String keyId(){return keyId;}

 public String createOrder(long amountPaise, long localOrderId) {
  if(!configured()) throw new GatewayException("Online payments are not configured. Set Razorpay test keys to enable checkout.");
  try {
   var body=json.createObjectNode().put("amount",amountPaise).put("currency","INR").put("receipt","TNAC-"+localOrderId);
   body.putObject("notes").put("artisanconnect_order_id",Long.toString(localOrderId));
   JsonNode response=request("POST","https://api.razorpay.com/v1/orders",body.toString());
   String id=response.path("id").asText("");
   if(!id.matches("order_[A-Za-z0-9]+")) throw new GatewayException("Payment provider did not return a valid order reference.");
   return id;
  } catch(GatewayException ex){throw ex;} catch(Exception ex){throw new GatewayException("Could not start payment with Razorpay. Please retry.",ex);}
 }

 public boolean verifyCaptured(String providerOrderId,String paymentId,String signature,long expectedPaise) {
  if(!configured() || providerOrderId==null || !providerOrderId.matches("order_[A-Za-z0-9]+")
      || paymentId==null || !paymentId.matches("pay_[A-Za-z0-9]+")
      || signature==null || !signature.matches("[0-9a-fA-F]{64}")) return false;
  try {
   String expected=sign(providerOrderId+"|"+paymentId,keySecret);
   if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),signature.getBytes(StandardCharsets.US_ASCII))) return false;
   JsonNode payment=request("GET","https://api.razorpay.com/v1/payments/"+paymentId,null);
   return "captured".equals(payment.path("status").asText())
     && providerOrderId.equals(payment.path("order_id").asText())
     && payment.path("amount").asLong(-1)==expectedPaise
     && "INR".equals(payment.path("currency").asText());
  } catch(Exception ex){return false;}
 }

 public boolean verifyWebhook(byte[] body,String signature) {
  if(!webhookConfigured() || body==null || signature==null || !signature.matches("[0-9a-fA-F]{64}")) return false;
  try {
   String expected=sign(body,webhookSecret);
   return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),signature.getBytes(StandardCharsets.US_ASCII));
  } catch(Exception ex){return false;}
 }
 public JsonNode parseWebhook(byte[] body) {try{return json.readTree(body);}catch(Exception ex){throw new GatewayException("Invalid payment webhook payload.",ex);}}
 private JsonNode request(String method,String url,String body)throws Exception {
  String auth=Base64.getEncoder().encodeToString((keyId+":"+keySecret).getBytes(StandardCharsets.UTF_8));
  var builder=HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(15)).header("Authorization","Basic "+auth).header("Accept","application/json");
  if(body==null) builder.GET(); else builder.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body));
  var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()<200||response.statusCode()>=300) throw new GatewayException("Payment provider returned HTTP "+response.statusCode()+".");
  return json.readTree(response.body());
 }
 private static String sign(String value,String secret)throws Exception {
  return sign(value.getBytes(StandardCharsets.UTF_8),secret);
 }
 private static String sign(byte[] value,String secret)throws Exception {
  Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
  return java.util.HexFormat.of().formatHex(mac.doFinal(value));
 }
 public static class GatewayException extends RuntimeException { public GatewayException(String message){super(message);} public GatewayException(String message,Throwable cause){super(message,cause);} }
}
