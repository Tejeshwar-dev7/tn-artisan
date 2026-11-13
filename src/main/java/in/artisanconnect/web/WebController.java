package in.artisanconnect.web;

import in.artisanconnect.domain.*; import in.artisanconnect.repo.*; import in.artisanconnect.service.FinancialCalculator; import in.artisanconnect.service.RazorpayGateway; import jakarta.servlet.http.*; import jakarta.validation.constraints.*; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Controller; import org.springframework.transaction.annotation.Transactional; import org.springframework.validation.annotation.Validated; import org.springframework.ui.Model; import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.support.RedirectAttributes; import java.math.BigDecimal; import java.time.Instant; import java.time.temporal.ChronoUnit; import java.util.*;

@Controller @Validated
public class WebController {
 private final ProductRepository products; private final CategoryRepository categories; private final UserRepository users; private final ArtisanRepository artisans; private final OrderRepository orders; private final OrderEventRepository events; private final EarningsLedgerRepository ledger; private final PasswordEncoder encoder; private final FinancialCalculator finance; private final ReviewController reviews; private final RazorpayGateway gateway;
 public WebController(ProductRepository p,CategoryRepository c,UserRepository u,ArtisanRepository a,OrderRepository o,OrderEventRepository e,EarningsLedgerRepository l,PasswordEncoder pe,FinancialCalculator f,ReviewController rc,RazorpayGateway gateway){products=p;categories=c;users=u;artisans=a;orders=o;events=e;ledger=l;encoder=pe;finance=f;reviews=rc;this.gateway=gateway;}
 @GetMapping("/")
 String home(Model model, HttpSession session){
  var featured=products.findByActiveTrueOrderByCreatedAtDesc().stream().limit(8).toList();
  model.addAttribute("products",featured);
  model.addAttribute("categories",categories.findAll());
  model.addAttribute("basketCount",cart(session).values().stream().mapToLong(Integer::longValue).sum());
  model.addAttribute("recentReviews",reviews.recent(3));
  Map<Long,Double> ratings=new HashMap<>();
  for(Product product:featured)reviews.forProduct(product.id).stream()
    .mapToInt(row->((Number)row.get("rating")).intValue()).average()
    .ifPresent(rating->ratings.put(product.id,rating));
  model.addAttribute("ratings",ratings);
  return "home";
 }
 @GetMapping("/crafts") String crafts(){return "redirect:/about";}
 @GetMapping("/about") String about(){return "about";}
 @GetMapping("/faq") String faq(){return "faq";}
 @GetMapping("/mission") String mission(){return "mission";}
 @GetMapping("/for-sellers") String forSellers(){return "for-sellers";}
 @GetMapping("/for-customers") String forCustomers(){return "for-customers";}
 @GetMapping("/products") String catalog(@RequestParam(required=false) String q,@RequestParam(required=false) Long category,@RequestParam(required=false) String sort,@RequestParam(defaultValue="false") boolean inStock,@RequestParam(required=false) BigDecimal minPrice,@RequestParam(required=false) BigDecimal maxPrice,Model m){var list=new ArrayList<>((q==null||q.isBlank())?products.findByActiveTrueOrderByCreatedAtDesc():products.findByActiveTrueAndNameContainingIgnoreCaseOrActiveTrueAndDescriptionContainingIgnoreCase(q,q)); if(category!=null)list.removeIf(p->!p.category.id.equals(category));if(inStock)list.removeIf(p->p.stock<1);if(minPrice!=null)list.removeIf(p->p.price.compareTo(minPrice)<0);if(maxPrice!=null)list.removeIf(p->p.price.compareTo(maxPrice)>0);if("price-low".equals(sort))list.sort(Comparator.comparing(p->p.price));else if("price-high".equals(sort))list.sort(Comparator.comparing((Product p)->p.price).reversed());else if("name".equals(sort))list.sort(Comparator.comparing(p->p.name,String.CASE_INSENSITIVE_ORDER));m.addAttribute("products",list);m.addAttribute("categories",categories.findAll());m.addAttribute("q",q);m.addAttribute("selectedCategory",category);m.addAttribute("sort",sort);m.addAttribute("inStock",inStock);m.addAttribute("minPrice",minPrice);m.addAttribute("maxPrice",maxPrice);m.addAttribute("reviewStats",reviews.summaries());return "catalog";}
 @GetMapping("/products/{id}") String product(@PathVariable Long id,Authentication auth,Model m){Product p=products.findById(id).orElseThrow();if(!p.active)throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);m.addAttribute("product",p);var feedback=reviews.forProduct(id);m.addAttribute("reviews",feedback);m.addAttribute("canReview",reviews.eligible(id,auth));m.addAttribute("average",feedback.stream().mapToInt(row->((Number)row.get("rating")).intValue()).average().orElse(0));return "product";}
 @GetMapping("/passport/{id}") String passport(@PathVariable Long id,Model m){Product p=products.findById(id).orElseThrow();m.addAttribute("product",p);m.addAttribute("profile",artisans.findByUserId(p.artisan.id).orElse(null));return "passport";}
 @GetMapping("/artisan/{id}") String artisan(@PathVariable Long id,Model m){m.addAttribute("profile",artisans.findByUserId(id).orElseThrow());m.addAttribute("products",products.findByArtisanIdOrderByCreatedAtDesc(id).stream().filter(p->p.active).toList());return "artisan";}
 @GetMapping("/login")
 String login(Model model) {
  model.addAttribute("demoCustomerEmail", in.artisanconnect.config.DemoAccounts.CUSTOMER_EMAIL);
  model.addAttribute("demoMakerEmail", in.artisanconnect.config.DemoAccounts.MAKER_EMAIL);
  model.addAttribute("demoPassword", in.artisanconnect.config.DemoAccounts.PASSWORD);
  return "login";
 }
 @GetMapping("/login/admin") String adminLogin(Model m){m.addAttribute("loginRole","ADMIN");return "login";}
 @GetMapping("/login/customer") String customerLogin(Model m){return demoLogin(m, "CUSTOMER", in.artisanconnect.config.DemoAccounts.CUSTOMER_EMAIL);}
 @GetMapping("/login/artisan") String artisanLogin(Model m){return demoLogin(m, "ARTISAN", in.artisanconnect.config.DemoAccounts.MAKER_EMAIL);}
 private String demoLogin(Model model, String role, String demoEmail) {
  model.addAttribute("loginRole", role);
  model.addAttribute("demoEmail", demoEmail);
  model.addAttribute("demoPassword", in.artisanconnect.config.DemoAccounts.PASSWORD);
  return "login";
 }
 @GetMapping("/register") String register(Model m){m.addAttribute("preferredRole",null);return "register";}
 @GetMapping("/register/customer") String customerRegister(Model m){m.addAttribute("preferredRole","CUSTOMER");return "register";}
 @GetMapping("/register/artisan") String artisanRegister(Model m){m.addAttribute("preferredRole","ARTISAN");return "register";}
 @PostMapping("/register") String register(@RequestParam @NotBlank String name,@RequestParam @Email String email,@RequestParam @Size(min=8) String password,@RequestParam String role,@RequestParam(required=false) String shopName,@RequestParam(required=false) String craft,@RequestParam(required=false) String district,RedirectAttributes ra){
  if (in.artisanconnect.config.DemoAccounts.isDemo(email.strip())) {
   ra.addFlashAttribute("error", "Use your own email address to register.");
   return "ARTISAN".equals(role) ? "redirect:/register/artisan" : "redirect:/register/customer";
  }
  var existing=users.findByEmailIgnoreCase(email);if(existing.isPresent()){ra.addFlashAttribute("success","An account already exists for this email. Please sign in.");ra.addFlashAttribute("email",email);return "redirect:/login/"+(existing.get().role==User.Role.ARTISAN?"artisan":"customer");}
  User.Role r=("ARTISAN".equals(role)?User.Role.ARTISAN:User.Role.CUSTOMER);User u=users.save(new User(name.trim(),email.trim(),encoder.encode(password),r));
  if(r==User.Role.ARTISAN){ArtisanProfile p=new ArtisanProfile();p.user=u;p.shopName=(shopName==null||shopName.isBlank())?name+"'s Studio":shopName.trim();p.craft=craft;p.district=district;p.story="Tell customers about the people and traditions behind your work.";artisans.save(p);}
  ra.addFlashAttribute("success",r==User.Role.ARTISAN?"Account created. Your artisan profile is pending admin verification. Please log in.":"Welcome to ArtisanConnect. Please log in.");return "redirect:/login";
 }
 @GetMapping("/account") String account(Authentication auth,Model m){User u=current(auth);if(u.role==User.Role.ADMIN)return "redirect:/admin";if(u.role==User.Role.ARTISAN)return "redirect:/artisan-area";if(u.role==User.Role.COORDINATOR)return "redirect:/coordinator";m.addAttribute("customer",u);m.addAttribute("orders",orders.findByCustomerIdOrderByCreatedAtDesc(u.id));return "customer";}
 @GetMapping("/customer/profile") String customerProfile(Authentication auth,Model m){m.addAttribute("customer",current(auth));return "customer-profile";}
 @PostMapping("/customer/profile") String saveCustomerProfile(Authentication auth,@RequestParam @NotBlank @Size(max=120) String name,@RequestParam @Pattern(regexp="[+0-9() -]{7,25}") String phone,@RequestParam @NotBlank @Size(max=180) String addressLine1,@RequestParam(required=false) @Size(max=180) String addressLine2,@RequestParam @NotBlank @Size(max=100) String city,@RequestParam @NotBlank @Size(max=100) String district,@RequestParam @NotBlank @Size(max=100) String state,@RequestParam @Pattern(regexp="[1-9][0-9]{5}") String postalCode,RedirectAttributes ra){User u=current(auth);u.name=name.strip();u.phone=phone.strip();u.addressLine1=addressLine1.strip();u.addressLine2=addressLine2==null?"":addressLine2.strip();u.city=city.strip();u.district=district.strip();u.state=state.strip();u.postalCode=postalCode.strip();users.save(u);ra.addFlashAttribute("success","Your customer profile has been saved.");return "redirect:/customer/profile";}
 @GetMapping("/artisan/profile") String artisanProfile(Authentication auth,Model m){User u=current(auth);m.addAttribute("user",u);m.addAttribute("profile",artisans.findByUserId(u.id).orElseThrow());return "artisan-profile";}
 @PostMapping("/artisan/profile") String saveArtisanProfile(Authentication auth,@RequestParam @NotBlank @Size(max=120) String shopName,@RequestParam @Size(max=80) String craft,@RequestParam @Size(max=120) String district,@RequestParam @Size(max=600) String story,@RequestParam @Pattern(regexp="[+0-9() -]{7,25}") String phone,RedirectAttributes ra){User u=current(auth);ArtisanProfile p=artisans.findByUserId(u.id).orElseThrow();p.shopName=shopName.strip();p.craft=craft==null?"":craft.strip();p.district=district==null?"":district.strip();p.story=story==null?"":story.strip();p.phone=phone.strip();artisans.save(p);ra.addFlashAttribute("success","Your artisan profile has been saved. Approval status remains "+p.status+".");return "redirect:/artisan/profile";}
 @PostMapping("/cart/add/{id}")
 String add(@PathVariable Long id,@RequestParam(defaultValue="1") int quantity,HttpSession session,RedirectAttributes flash){
  Product product=products.findById(id).orElse(null);
  Map<Long,Integer> basket=cart(session);
  long requested=(long)basket.getOrDefault(id,0)+quantity;
  if(product==null||!product.active||quantity<1||requested>product.stock){
   flash.addFlashAttribute("error","That quantity is unavailable. Check the listing's current stock.");
  }else{
   basket.put(id,(int)requested);
   flash.addFlashAttribute("success","Added to your basket.");
  }
  return "redirect:/cart";
 }

 @GetMapping("/cart") String basket(HttpSession session,Model model){
  var rows=cartRows(session);
  model.addAttribute("rows",rows);
  model.addAttribute("total",rows.stream().map(r->(BigDecimal)r.get("lineTotal")).reduce(BigDecimal.ZERO,BigDecimal::add));
  return "cart";
 }

 @PostMapping("/cart/update")
 String update(@RequestParam Long productId,@RequestParam int quantity,HttpSession session,RedirectAttributes flash){
  Map<Long,Integer> basket=cart(session);
  if(quantity==0){basket.remove(productId);return "redirect:/cart";}
  Product product=products.findById(productId).orElse(null);
  if(quantity<0||!basket.containsKey(productId)||product==null||!product.active||quantity>product.stock){
   flash.addFlashAttribute("error","Choose a quantity within the available stock, or enter zero to remove the item.");
  }else basket.put(productId,quantity);
  return "redirect:/cart";
 }
 @GetMapping("/checkout") String checkout(HttpSession s,Authentication a,Model m){if(a==null)return "redirect:/login/customer";if(current(a).role!=User.Role.CUSTOMER)return "redirect:/cart?customerRequired";if(cart(s).isEmpty())return "redirect:/products";m.addAttribute("rows",cartRows(s));m.addAttribute("total",cartTotal(s));m.addAttribute("customer",current(a));m.addAttribute("razorpayConfigured",gateway.configured());return "checkout";}

 @Transactional @PostMapping("/checkout") String prepare(
  @RequestParam @NotBlank @Size(max=120) String recipientName,
  @RequestParam @Pattern(regexp="[+0-9() -]{7,25}") String phone,
  @RequestParam @NotBlank @Size(max=180) String addressLine1,
  @RequestParam(required=false) @Size(max=180) String addressLine2,
  @RequestParam @NotBlank @Size(max=100) String city,
  @RequestParam @NotBlank @Size(max=100) String district,
  @RequestParam @NotBlank @Size(max=100) String state,
  @RequestParam @Pattern(regexp="[1-9][0-9]{5}") String postalCode,
  @RequestParam(defaultValue="RAZORPAY") String paymentMethod,
  HttpSession session,Authentication auth,RedirectAttributes ra){
  if(!Set.of("RAZORPAY","COD").contains(paymentMethod)){ra.addFlashAttribute("error","Choose a supported payment method.");return "redirect:/checkout";}
  if("RAZORPAY".equals(paymentMethod)&&!gateway.configured()){ra.addFlashAttribute("error","Online payments are not configured yet. A site administrator must add Razorpay test keys.");return "redirect:/checkout";}
  Map<Long,Integer> basket=new TreeMap<>(cart(session));if(basket.isEmpty())return "redirect:/cart";
  PurchaseOrder order=new PurchaseOrder();order.customer=current(auth);order.recipientName=recipientName.strip();order.deliveryPhone=phone.strip();order.addressLine1=addressLine1.strip();order.addressLine2=addressLine2==null?"":addressLine2.strip();order.deliveryCity=city.strip();order.deliveryDistrict=district.strip();order.deliveryState=state.strip();order.deliveryPostalCode=postalCode.strip();String addressSummary=String.join(", ",Arrays.stream(new String[]{order.addressLine1,order.addressLine2,order.deliveryCity,order.deliveryDistrict,order.deliveryState,order.deliveryPostalCode}).filter(x->x!=null&&!x.isBlank()).toList());order.shippingAddress=addressSummary.substring(0,Math.min(255,addressSummary.length()));order.expectedDeliveryAt=Instant.now().plus(14,ChronoUnit.DAYS);
  BigDecimal total=BigDecimal.ZERO;
  for(var entry:basket.entrySet()){
   Product p=products.lockById(entry.getKey()).orElse(null);int qty=entry.getValue();
   if(p==null||!p.active||qty<1||p.stock<qty){ra.addFlashAttribute("error","Stock has changed. Review your basket before checkout.");return "redirect:/cart";}
   BigDecimal line=p.price.multiply(BigDecimal.valueOf(qty));total=total.add(line);
   PurchaseItem item=new PurchaseItem();item.order=order;item.product=p;item.artisan=p.artisan;item.quantity=qty;item.unitPrice=p.price;item.commission=finance.commission(line);item.artisanEarnings=finance.artisanShare(line);order.items.add(item);
  }
  order.total=total;order.paymentMethod=paymentMethod;
  if("COD".equals(paymentMethod)){
   order.status=PurchaseOrder.Status.ORDER_PLACED;order.paymentStatus="COD_PENDING";orders.save(order);
   for(PurchaseItem item:order.items)item.product.stock-=item.quantity;products.saveAll(order.items.stream().map(i->i.product).distinct().toList());
   events.save(new OrderEvent(order,PurchaseOrder.Status.ORDER_PLACED,"Order placed. Cash payment is due on delivery."));cart(session).clear();return "redirect:/customer/orders/"+order.id;
  }
  order.status=PurchaseOrder.Status.PAYMENT_PENDING;order.paymentStatus="PENDING";orders.save(order);events.save(new OrderEvent(order,PurchaseOrder.Status.PAYMENT_PENDING,"Checkout started; awaiting verified payment."));
  try{long paise=total.movePointRight(2).longValueExact();order.providerOrderId=gateway.createOrder(paise,order.id);orders.save(order);return "redirect:/checkout/payment/"+order.id;}
  catch(Exception ex){order.status=PurchaseOrder.Status.PAYMENT_FAILED;order.paymentStatus="FAILED";orders.save(order);events.save(new OrderEvent(order,PurchaseOrder.Status.PAYMENT_FAILED,"Online payment could not be started. No payment was recorded."));ra.addFlashAttribute("error",ex.getMessage());return "redirect:/customer/orders/"+order.id;}
 }

 @GetMapping("/checkout/payment/{id}") String payment(@PathVariable Long id,Authentication auth,Model model,RedirectAttributes ra){
  PurchaseOrder order=ownedOrder(id,auth);if(order.status!=PurchaseOrder.Status.PAYMENT_PENDING||order.providerOrderId==null)return "redirect:/customer/orders/"+id;
  model.addAttribute("order",order);model.addAttribute("keyId",gateway.keyId());model.addAttribute("amountPaise",order.total.movePointRight(2).longValueExact());return "payment";
 }

 @Transactional @PostMapping("/checkout/verify") String verifyPayment(@RequestParam Long orderId,@RequestParam("razorpay_order_id") String providerOrderId,@RequestParam("razorpay_payment_id") String paymentId,@RequestParam("razorpay_signature") String signature,Authentication auth,HttpSession session,RedirectAttributes ra){
  ownedOrder(orderId,auth);PurchaseOrder order=orders.lockById(orderId).orElseThrow();if(order.status!=PurchaseOrder.Status.PAYMENT_PENDING||!Objects.equals(order.providerOrderId,providerOrderId)){ra.addFlashAttribute("error","This payment session is no longer active.");return "redirect:/customer/orders/"+orderId;}
  long expectedPaise=order.total.movePointRight(2).longValueExact();
  if(!gateway.verifyCaptured(providerOrderId,paymentId,signature,expectedPaise)){ra.addFlashAttribute("error","Payment has not been confirmed by the provider. The order remains unfulfilled until its status is verified.");return "redirect:/customer/orders/"+orderId;}
  if("PAID".equals(order.paymentStatus))return "redirect:/customer/orders/"+orderId;
  List<Product> locked=new ArrayList<>();for(PurchaseItem item:order.items){Product p=products.lockById(item.product.id).orElseThrow();if(!p.active||p.stock<item.quantity){order.paymentStatus="PAID";order.paymentReference=paymentId;order.status=PurchaseOrder.Status.PAYMENT_PENDING;orders.save(order);events.save(new OrderEvent(order,PurchaseOrder.Status.PAYMENT_CONFIRMED,"Payment captured. Fulfilment is on hold while stock availability is resolved."));ra.addFlashAttribute("error","Payment was received, but stock needs a manual check. Contact support with this order ID.");return "redirect:/customer/orders/"+orderId;}locked.add(p);}
  for(int i=0;i<order.items.size();i++){PurchaseItem item=order.items.get(i);Product p=locked.get(i);p.stock-=item.quantity;item.product=p;EarningsLedger l=new EarningsLedger();l.orderItemId=item.id;l.artisan=item.artisan;l.order=order;l.grossAmount=item.unitPrice.multiply(BigDecimal.valueOf(item.quantity));l.platformCommission=item.commission;l.artisanAmount=item.artisanEarnings;ledger.save(l);}
  products.saveAll(locked);order.paymentReference=paymentId;order.paymentStatus="PAID";order.status=PurchaseOrder.Status.ORDER_PLACED;orders.save(order);events.save(new OrderEvent(order,PurchaseOrder.Status.PAYMENT_CONFIRMED,"Razorpay confirmed the captured payment."));events.save(new OrderEvent(order,PurchaseOrder.Status.ORDER_PLACED,"Order placed; awaiting artisan acceptance."));cart(session).clear();return "redirect:/customer/orders/"+orderId;
 }

 @PostMapping("/checkout/payment-failed") String paymentFailed(@RequestParam Long orderId,@RequestParam(required=false) String reason,Authentication auth,RedirectAttributes ra){PurchaseOrder order=ownedOrder(orderId,auth);if(order.status==PurchaseOrder.Status.PAYMENT_PENDING&&!"PAID".equals(order.paymentStatus)){order.status=PurchaseOrder.Status.PAYMENT_FAILED;order.paymentStatus="FAILED";orders.save(order);events.save(new OrderEvent(order,PurchaseOrder.Status.PAYMENT_FAILED,"Payment attempt failed or was cancelled."));}ra.addFlashAttribute("error","Payment was not completed. No order fulfilment has started.");return "redirect:/customer/orders/"+orderId;}

 @Transactional @PostMapping(value="/webhooks/razorpay",consumes=MediaType.APPLICATION_JSON_VALUE) @ResponseBody ResponseEntity<String> razorpayWebhook(@RequestBody byte[] body,@RequestHeader(value="X-Razorpay-Signature",required=false) String signature){
  if(!gateway.verifyWebhook(body,signature))return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
   try{var root=gateway.parseWebhook(body);String event=root.path("event").asText();var entity=root.path("payload").path("payment").path("entity");String providerId=entity.path("order_id").asText("");String paymentId=entity.path("id").asText("");Optional<PurchaseOrder> found=orders.lockByProviderOrderId(providerId);if(found.isEmpty())return ResponseEntity.ok("ignored");PurchaseOrder order=found.get();
   if("payment.captured".equals(event)&&entity.path("status").asText().equals("captured")&&entity.path("amount").asLong(-1)==order.total.movePointRight(2).longValueExact()&&"INR".equals(entity.path("currency").asText())){completeWebhookPayment(order,paymentId);}
   else if("payment.failed".equals(event)&&order.status==PurchaseOrder.Status.PAYMENT_PENDING){order.status=PurchaseOrder.Status.PAYMENT_FAILED;order.paymentStatus="FAILED";orders.save(order);events.save(new OrderEvent(order,PurchaseOrder.Status.PAYMENT_FAILED,"Razorpay reported a failed payment."));}
   return ResponseEntity.ok("accepted");
  }catch(Exception ex){return ResponseEntity.badRequest().body("invalid event");}
 }
 @Transactional void completeWebhookPayment(PurchaseOrder order,String paymentId){if("PAID".equals(order.paymentStatus))return;List<Product> locked=new ArrayList<>();for(PurchaseItem item:order.items){Product p=products.lockById(item.product.id).orElseThrow();if(!p.active||p.stock<item.quantity){events.save(new OrderEvent(order,PurchaseOrder.Status.PAYMENT_CONFIRMED,"Payment captured by webhook; stock reconciliation is required."));order.paymentStatus="PAID";order.paymentReference=paymentId;orders.save(order);return;}locked.add(p);}for(int i=0;i<order.items.size();i++){PurchaseItem item=order.items.get(i);Product p=locked.get(i);p.stock-=item.quantity;products.save(p);item.product=p;EarningsLedger l=new EarningsLedger();l.orderItemId=item.id;l.artisan=item.artisan;l.order=order;l.grossAmount=item.unitPrice.multiply(BigDecimal.valueOf(item.quantity));l.platformCommission=item.commission;l.artisanAmount=item.artisanEarnings;ledger.save(l);}order.paymentStatus="PAID";order.paymentReference=paymentId;order.status=PurchaseOrder.Status.ORDER_PLACED;orders.save(order);events.save(new OrderEvent(order,PurchaseOrder.Status.PAYMENT_CONFIRMED,"Razorpay webhook confirmed payment capture."));events.save(new OrderEvent(order,PurchaseOrder.Status.ORDER_PLACED,"Order placed; awaiting artisan acceptance."));}
 @GetMapping("/customer/orders/{id}") String order(@PathVariable Long id,Authentication auth,Model m){PurchaseOrder o=ownedOrder(id,auth);m.addAttribute("order",o);m.addAttribute("events",events.findByOrderIdOrderByCreatedAtAsc(id));m.addAttribute("reviewable",o.status==PurchaseOrder.Status.DELIVERED);return "order";}
 @GetMapping(value="/customer/orders/{id}/tracking-data",produces=MediaType.APPLICATION_JSON_VALUE) @ResponseBody ResponseEntity<Map<String,Object>> trackingData(@PathVariable Long id,Authentication auth){PurchaseOrder o=ownedOrder(id,auth);Map<String,Object> data=new LinkedHashMap<>();data.put("orderId",o.id);data.put("status",o.status.name());data.put("paymentStatus",o.paymentStatus);data.put("courier",o.courierName);data.put("trackingNumber",o.trackingNumber);data.put("latestLocation",o.latestShipmentLocation);data.put("trackingUrl",o.externalTrackingUrl);data.put("events",events.findByOrderIdOrderByCreatedAtAsc(id).stream().map(e->{Map<String,String> row=new LinkedHashMap<>();row.put("status",e.status==null?"UPDATE":e.status.name());row.put("note",e.note==null?"":e.note);row.put("createdAt",e.createdAt.toString());return row;}).toList());return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data);}

 @Transactional(readOnly=true) @GetMapping("/artisan-area/summary") @ResponseBody
 ResponseEntity<Map<String,Object>> sellerSummary(Authentication auth){
  User maker=current(auth); BigDecimal confirmed=BigDecimal.ZERO,pending=BigDecimal.ZERO,commission=BigDecimal.ZERO;
  int count=0; List<Map<String,Object>> recent=new ArrayList<>();
  for(PurchaseOrder order:orders.findDistinctByItemsArtisanIdOrderByCreatedAtDesc(maker.id)){
   if(!Set.of("PAID","COD_PENDING").contains(order.paymentStatus)||order.status==PurchaseOrder.Status.CANCELLED||order.status==PurchaseOrder.Status.REFUNDED)continue;
   count++;BigDecimal gross=BigDecimal.ZERO,net=BigDecimal.ZERO,fee=BigDecimal.ZERO;
   for(PurchaseItem item:order.items)if(item.artisan.id.equals(maker.id)){gross=gross.add(item.unitPrice.multiply(BigDecimal.valueOf(item.quantity)));net=net.add(item.artisanEarnings);fee=fee.add(item.commission);}
   if("PAID".equals(order.paymentStatus)){confirmed=confirmed.add(net);commission=commission.add(fee);}else pending=pending.add(net);
   Map<String,Object> row=new LinkedHashMap<>();row.put("id",order.id);row.put("status",order.status.name());row.put("paymentStatus",order.paymentStatus);row.put("gross",gross);row.put("commission",fee);row.put("net",net);recent.add(row);
  }
  Map<String,Object> result=new LinkedHashMap<>();result.put("orders",count);result.put("confirmed",confirmed);result.put("pending",pending);result.put("commission",commission);result.put("recent",recent);result.put("updatedAt",Instant.now().toString());
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
 }
 @GetMapping("/artisan-area") String artisanArea(Authentication auth,Model m){User u=current(auth);var profile=artisans.findByUserId(u.id).orElseThrow();m.addAttribute("profile",profile);m.addAttribute("products",products.findByArtisanIdOrderByCreatedAtDesc(u.id));m.addAttribute("orders",orders.findDistinctByItemsArtisanIdOrderByCreatedAtDesc(u.id).stream().filter(o->Set.of("PAID","COD_PENDING").contains(o.paymentStatus)).toList());m.addAttribute("categories",categories.findAll());m.addAttribute("ledger",ledger.findByArtisanIdOrderByRecordedAtDesc(u.id));return "artisan-dashboard";}
 @PostMapping("/artisan-area/product") String createProduct(Authentication auth,@RequestParam @NotBlank @Size(max=160) String name,@RequestParam @NotBlank @Size(max=3000) String description,@RequestParam @Positive BigDecimal price,@RequestParam @Min(0) int stock,@RequestParam @NotBlank @Size(max=160) String materials,@RequestParam @NotBlank @Size(max=100) String technique,@RequestParam @NotBlank @Size(max=80) String district,@RequestParam @Positive Long categoryId,@RequestParam(required=false) @Size(max=180) String dimensions,@RequestParam(required=false) String imageUrl,RedirectAttributes ra){User u=current(auth);if(artisans.findByUserId(u.id).filter(p->p.status==ArtisanProfile.Verification.APPROVED).isEmpty()){ra.addFlashAttribute("error","Your profile must be approved before publishing products.");return "redirect:/artisan-area";}if(imageUrl!=null&&!imageUrl.isBlank()&&!imageUrl.trim().matches("https://[^\\s]+")){ra.addFlashAttribute("error","Product images must use a valid HTTPS image URL.");return "redirect:/artisan-area";}Product p=new Product();p.name=name.trim();p.description=description.trim();p.price=price;p.stock=stock;p.materials=materials;p.technique=technique;p.district=district;p.category=categories.findById(categoryId).orElseThrow();p.artisan=u;p.dimensions=dimensions==null||dimensions.isBlank()?"Confirm with the maker":dimensions.strip();p.imageUrl=imageUrl==null||imageUrl.isBlank()?imageFor(name,p.category.name):imageUrl.trim();p.imageCredit=imageUrl==null||imageUrl.isBlank()?"TN ARTISAN craft reference":"Provided by the artisan";products.save(p);ra.addFlashAttribute("success","Product published to the marketplace.");return "redirect:/artisan-area";}
 @Transactional @PostMapping("/artisan-area/order/{id}/status") String updateOrder(@PathVariable Long id,Authentication auth,@RequestParam PurchaseOrder.Status status,@RequestParam(required=false) String note,@RequestParam(required=false) String courierName,@RequestParam(required=false) String trackingNumber,@RequestParam(required=false) String trackingUrl,@RequestParam(required=false) String latestLocation,RedirectAttributes ra){User u=current(auth);PurchaseOrder o=orders.findDetailedById(id).orElseThrow();if(o.items.stream().noneMatch(i->i.artisan.id.equals(u.id)))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);if(!allowed(o.status,status)){ra.addFlashAttribute("error","That order status transition is not allowed.");return "redirect:/artisan-area";}if(status==PurchaseOrder.Status.SHIPPED&&(courierName==null||courierName.isBlank()||trackingNumber==null||trackingNumber.isBlank())){ra.addFlashAttribute("error","Enter the delivery partner and tracking reference before marking the parcel shipped.");return "redirect:/artisan-area";}if(trackingUrl!=null&&!trackingUrl.isBlank()&&!trackingUrl.matches("https://[^\\s]+")){ra.addFlashAttribute("error","Tracking links must use HTTPS.");return "redirect:/artisan-area";}o.status=status;if(courierName!=null&&!courierName.isBlank())o.courierName=courierName.strip();if(trackingNumber!=null&&!trackingNumber.isBlank())o.trackingNumber=trackingNumber.strip();if(trackingUrl!=null&&!trackingUrl.isBlank())o.externalTrackingUrl=trackingUrl.strip();if(latestLocation!=null&&!latestLocation.isBlank())o.latestShipmentLocation=latestLocation.strip();if(status==PurchaseOrder.Status.SHIPPED){o.dispatchedAt=Instant.now();if(o.expectedDeliveryAt==null||o.expectedDeliveryAt.isBefore(Instant.now()))o.expectedDeliveryAt=Instant.now().plus(5,ChronoUnit.DAYS);}if(status==PurchaseOrder.Status.DELIVERED)o.deliveredAt=Instant.now();orders.save(o);String update=note==null?"":note.strip();if(status==PurchaseOrder.Status.SHIPPED)update=(update.isBlank()?"Parcel handed to delivery partner.":update)+" "+o.courierName+" tracking reference "+o.trackingNumber+".";if(o.latestShipmentLocation!=null&&!o.latestShipmentLocation.isBlank())update+=" Last reported location: "+o.latestShipmentLocation+".";events.save(new OrderEvent(o,status,update.isBlank()?"Order status updated by the artisan.":update));return "redirect:/artisan-area";}
 @GetMapping("/admin") String admin(Model m){m.addAttribute("pending",artisans.findByStatusOrderByIdDesc(ArtisanProfile.Verification.PENDING));m.addAttribute("orders",orders.findAll());m.addAttribute("users",users.count());m.addAttribute("products",products.count());m.addAttribute("accounts",users.findAll());return "admin";}
 @PostMapping("/admin/artisan/{id}") String verify(@PathVariable Long id,@RequestParam String decision,@RequestParam(required=false) String note){ArtisanProfile p=artisans.findById(id).orElseThrow();p.status="approve".equals(decision)?ArtisanProfile.Verification.APPROVED:ArtisanProfile.Verification.REJECTED;p.verificationNote=note;artisans.save(p);return "redirect:/admin";}
 @PostMapping("/admin/order/{id}") String adminOrder(@PathVariable Long id,@RequestParam PurchaseOrder.Status status,@RequestParam(required=false) String note){PurchaseOrder o=orders.findById(id).orElseThrow();o.status=status;orders.save(o);events.save(new OrderEvent(o,status,note));return "redirect:/admin";}
 private User current(Authentication a){return users.findByEmailIgnoreCase(a.getName()).orElseThrow();}
 @SuppressWarnings("unchecked") private Map<Long,Integer> cart(HttpSession s){Object obj=s.getAttribute("basket");if(obj==null){Map<Long,Integer> map=new LinkedHashMap<>();s.setAttribute("basket",map);return map;}return (Map<Long,Integer>)obj;}
 private boolean allowed(PurchaseOrder.Status from,PurchaseOrder.Status to){return switch(from){case ORDER_PLACED->to==PurchaseOrder.Status.ACCEPTED||to==PurchaseOrder.Status.CANCELLED;case ACCEPTED->to==PurchaseOrder.Status.PRODUCTION_STARTED||to==PurchaseOrder.Status.CANCELLED;case PRODUCTION_STARTED->to==PurchaseOrder.Status.QUALITY_CHECK;case QUALITY_CHECK->to==PurchaseOrder.Status.PACKED;case PACKED->to==PurchaseOrder.Status.SHIPPED;case SHIPPED->to==PurchaseOrder.Status.OUT_FOR_DELIVERY;case OUT_FOR_DELIVERY->to==PurchaseOrder.Status.DELIVERED;default->false;};}
 private PurchaseOrder ownedOrder(Long id,Authentication auth){PurchaseOrder o=orders.findDetailedById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));User u=current(auth);if(!o.customer.id.equals(u.id)&&u.role!=User.Role.ADMIN)throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);return o;}
 private List<Map<String,Object>> cartRows(HttpSession s){List<Map<String,Object>> rows=new ArrayList<>();for(var e:cart(s).entrySet())products.findById(e.getKey()).ifPresent(p->{Map<String,Object> row=new HashMap<>();row.put("product",p);row.put("quantity",e.getValue());row.put("lineTotal",p.price.multiply(BigDecimal.valueOf(e.getValue())));rows.add(row);});return rows;}
 private BigDecimal cartTotal(HttpSession s){BigDecimal total=BigDecimal.ZERO;for(var row:cartRows(s))total=total.add(((Product)row.get("product")).price.multiply(BigDecimal.valueOf((Integer)row.get("quantity"))));return total;}
 private String imageFor(String name,String category){String c=category.toLowerCase(Locale.ROOT);if(c.contains("painting"))return "/images/shiva-parvati-oval.png";if(c.contains("bronze"))return "/images/commons/swamimalai-bronze.jpg";if(c.contains("brass"))return "/images/commons/tamil-brass-lamp.jpg";if(c.contains("clay")||c.contains("pottery"))return "/images/potter-and-market.png";if(c.contains("kalamkari")||c.contains("textile"))return "/images/kalamkari-artisan.png";if(c.contains("palm")||c.contains("jute"))return "/images/commons/basket-weaving.jpg";if(c.contains("wood"))return "/images/commons/temple-wood-carving.jpg";if(c.contains("stone"))return "/images/wood-carver.png";return "/images/thanjavur-painting-process.png";}
}
