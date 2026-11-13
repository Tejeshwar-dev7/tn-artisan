package in.artisanconnect.config;

import in.artisanconnect.domain.*;
import in.artisanconnect.repo.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedData {
 @Bean CommandLineRunner seed(UserRepository users, CategoryRepository categories, ProductRepository products,
   ArtisanRepository artisans, PasswordEncoder encoder,
   @Value("${APP_ADMIN_EMAIL:}") String configuredEmail,
   @Value("${APP_ADMIN_PASSWORD:}") String configuredPassword,
   @Value("${app.public-hosting:false}") boolean publicHosting) {
  return args -> {
   if (publicHosting && (configuredEmail.isBlank() || configuredPassword.length() < 16)) {
    throw new IllegalStateException("Set APP_ADMIN_EMAIL and an APP_ADMIN_PASSWORD of at least 16 characters.");
   }
   String adminEmail = configuredEmail.isBlank()
     ? users.findAll().stream().filter(u -> u.role == User.Role.ADMIN).map(u -> u.email).findFirst().orElse("admin@artisanconnect.local")
     : configuredEmail.strip().toLowerCase(Locale.ROOT);
   var configuredAdmin = users.findByEmailIgnoreCase(adminEmail);
   if (configuredAdmin.isPresent() && configuredAdmin.get().role != User.Role.ADMIN) {
    throw new IllegalStateException("The configured administrator email belongs to an existing non-admin account.");
   }
   if (configuredAdmin.isEmpty()) {
    var previousAdmin = users.findByEmailIgnoreCase("admin@artisanconnect.local").filter(u -> u.role == User.Role.ADMIN);
    User admin = previousAdmin.orElseGet(() -> new User("Marketplace Admin", adminEmail, encoder.encode(configuredPassword.isBlank() ? "Admin@123" : configuredPassword), User.Role.ADMIN));
    admin.email = adminEmail;
    users.save(admin);
   }
   seedDemoAccount(users, encoder, "Demo Customer", DemoAccounts.CUSTOMER_EMAIL, User.Role.CUSTOMER);
   User demoMaker = seedDemoAccount(users, encoder, "Demo Maker", DemoAccounts.MAKER_EMAIL, User.Role.ARTISAN);
   ensureProfile(artisans, demoMaker, "Demo Maker Studio", "Wood carving", "Thanjavur",
     "A sample studio for exploring the maker dashboard.");
   User artisan = users.findByEmailIgnoreCase("meenakshi@artisanconnect.local").orElseGet(() -> users.save(
     new User("Meenakshi", "meenakshi@artisanconnect.local", encoder.encode(publicHosting ? UUID.randomUUID().toString() : "Artisan@123"), User.Role.ARTISAN)));
   ensureProfile(artisans, artisan, "Kaveri Kalai", "Thanjavur painting", "Thanjavur",
     "A family studio carrying forward the layered painting tradition of the Kaveri delta.");
   User artisan2 = users.findByEmailIgnoreCase("arun@artisanconnect.local").orElseGet(() -> users.save(
     new User("Arun", "arun@artisanconnect.local", encoder.encode(publicHosting ? UUID.randomUUID().toString() : "Artisan@123"), User.Role.ARTISAN)));
   ensureProfile(artisans, artisan2, "Swamimalai Sthapathi Works", "Bronze icon casting", "Thanjavur",
     "Hand-finished bronze icons shaped through the traditional lost-wax craft of Swamimalai.");
   User studio = users.findByEmailIgnoreCase("studio@tnartisan.local").orElseGet(() -> users.save(
     new User("TN ARTISAN Studio", "studio@tnartisan.local", encoder.encode(UUID.randomUUID().toString()), User.Role.ARTISAN)));
   ensureProfile(artisans, studio, "TN ARTISAN Studio", "Curated concept collection", "Tamil Nadu",
     "A demonstration storefront for exploring Tamil Nadu craft forms. Product examples, prices and sizes are illustrative and need confirmation from an artisan.");

   String[][] initialCategories = {
     {"Thanjavur Paintings", "Thanjavur"}, {"Thanjavur Art Plates", "Thanjavur"}, {"Swamimalai Bronze", "Swamimalai"},
     {"Nachiarkoil Brass Lamps", "Nachiarkoil"}, {"Kalamkari & Textiles", "Kumbakonam"},
     {"Mamallapuram Stone Carving", "Mamallapuram"}, {"Tamil Nadu Wood Carving", "Kallakurichi"},
     {"Clay & Pottery", "Tamil Nadu"}, {"Palm Leaf & Jute", "Tamil Nadu"}};
   Map<String, Category> categoryMap = new HashMap<>();
   categories.findAll().forEach(c -> categoryMap.put(c.name, c));
   for (String[] row : initialCategories) categoryMap.computeIfAbsent(row[0], name -> categories.save(new Category(name, row[1])));
   if (products.count() == 0) addStarterProducts(products, categoryMap, artisan, artisan2);

   Set<String> existingNames = new HashSet<>();
   products.findAll().forEach(p -> existingNames.add(p.name.toLowerCase(Locale.ROOT)));
   try (var aliases = new BufferedReader(new InputStreamReader(new ClassPathResource("product-photos.csv").getInputStream(), StandardCharsets.UTF_8))) {
    String line;
    while ((line = aliases.readLine()) != null) {
     String[] photo = line.split("\\|", -1);
     if (photo.length == 5 && existingNames.contains(photo[1].toLowerCase(Locale.ROOT))) existingNames.add(photo[0].toLowerCase(Locale.ROOT));
    }
   }
   try (var input = new BufferedReader(new InputStreamReader(new ClassPathResource("marketplace-products.csv").getInputStream(), StandardCharsets.UTF_8))) {
    String line;
    while ((line = input.readLine()) != null) {
     if (line.isBlank() || line.startsWith("#")) continue;
     String[] f = line.split("\\|", -1);
     if (f.length != 9) throw new IllegalStateException("Expected 9 fields in marketplace-products.csv: " + line);
     String name = f[0].trim();
     if (!existingNames.add(name.toLowerCase(Locale.ROOT))) continue;
     String categoryName = canonicalCategory(f[1]);
     Category category = categoryMap.get(categoryName);
     if (category == null) {
      category = categories.save(new Category(categoryName, f[6]));
      categoryMap.put(categoryName, category);
     }
     Product p = new Product();
     p.name = name;
     p.category = category;
     p.price = new BigDecimal(f[2]);
     p.dimensions = f[3];
     p.materials = f[4];
     p.technique = f[5];
     p.district = f[6];
     p.imageUrl = f[7];
     p.description = f[8] + " " + craftDescription(f[1]) + " Prototype listing: price and dimensions are estimates for this project and must be confirmed with the maker before purchase.";
     p.stock = 8;
     p.artisan = studio;
     applyPhotoCredit(p);
     products.save(p);
    }
   }

   List<Product> currentProducts = products.findAll();
   for (Product p : currentProducts) {
    if ("Smoke Test Craft".equalsIgnoreCase(p.name)) p.active = false;
    if (p.category != null) p.category = categoryMap.getOrDefault(canonicalCategory(p.category.name), p.category);
    if (p.imageUrl == null || p.imageUrl.isBlank() || p.imageUrl.toLowerCase(Locale.ROOT).contains("poompuhar")) {
     p.imageUrl = imageFor(p.name, p.category == null ? "" : p.category.name);
     applyPhotoCredit(p);
    }
   }
   try (var input = new BufferedReader(new InputStreamReader(new ClassPathResource("product-photos.csv").getInputStream(), StandardCharsets.UTF_8))) {
    String line;
    while ((line = input.readLine()) != null) {
     String[] photo = line.split("\\|", -1);
     if (photo.length != 5) continue;
     for (Product product : currentProducts) {
      if (product.name.equals(photo[0]) || product.name.equals(photo[1])) {
       if (!photo[0].equals(photo[1])) {
        product.name = photo[1];
        product.description = photo[1] + ". The supplied photograph shows the detailed ornamental finish and sculptural silhouette of this decorative piece. Its layered forms reward close viewing and make it a focal point for a thoughtfully arranged interior. The casting and surface finishing require patient attention to small details. Price and dimensions are project estimates; confirm alloy, weight, final finish and measurements with the maker before ordering. Display on a stable surface and clean with a soft dry cloth. For lamps, confirm mounting and oil capacity and never leave a flame unattended.";
       }
       product.imageUrl = photo[2]; product.imageCredit = photo[3]; product.imageSource = photo[4];
      }
     }
    }
   }
   products.saveAll(currentProducts);
   Set<Long> usedCategoryIds = new HashSet<>();
   currentProducts.stream().filter(p -> p.category != null).forEach(p -> usedCategoryIds.add(p.category.id));
   Set<String> retiredCategoryNames = Set.of("Thanjavur Painting", "Bronze Icons", "Brass Lamps", "Wood Carving", "Stone Carving");
   categories.findAll().stream().filter(c -> retiredCategoryNames.contains(c.name) && !usedCategoryIds.contains(c.id)).forEach(categories::delete);
  };
 }

 private User seedDemoAccount(UserRepository users, PasswordEncoder encoder, String name, String email, User.Role role) {
  User user = users.findByEmailIgnoreCase(email).orElseGet(() ->
    users.save(new User(name, email, encoder.encode(DemoAccounts.PASSWORD), role)));
  if (user.role != role || !name.equals(user.name)) {
   throw new IllegalStateException("Reserved demo account email is already in use: " + email);
  }
  return user;
 }

 private void ensureProfile(ArtisanRepository profiles, User user, String shop, String craft, String district, String story) {
  if (profiles.findByUserId(user.id).isEmpty()) {
   ArtisanProfile profile = new ArtisanProfile();
   profile.user = user; profile.shopName = shop; profile.craft = craft; profile.district = district;
   profile.story = story; profile.status = ArtisanProfile.Verification.APPROVED; profiles.save(profile);
  }
 }

 private void addStarterProducts(ProductRepository products, Map<String, Category> categories, User artisan, User artisan2) {
  String[][] rows = {
   {"Lakshmi Thanjavur Painting", "Thanjavur Paintings", "4200", "Wood panel, gesso and gold-tone foil", "Thanjavur"},
   {"Swamimalai Bronze Nataraja", "Swamimalai Bronze", "6800", "Bronze", "Swamimalai"},
   {"Nachiarkoil Annam Lamp", "Nachiarkoil Brass Lamps", "2350", "Brass", "Nachiarkoil"},
   {"Thanjavur Art Plate", "Thanjavur Art Plates", "1850", "Copper alloy", "Thanjavur"},
   {"Mamallapuram Stone Ganesha", "Mamallapuram Stone Carving", "1600", "Granite", "Mamallapuram"},
   {"Kallakurichi Carved Panel", "Tamil Nadu Wood Carving", "3200", "Seasoned hardwood", "Kallakurichi"},
   {"Hand-thrown Terracotta Diyas", "Clay & Pottery", "450", "Terracotta clay", "Tamil Nadu"},
   {"Palm Leaf Woven Basket", "Palm Leaf & Jute", "780", "Palm leaf", "Tamil Nadu"}
  };
  for (int i=0;i<rows.length;i++) {
   String[] r=rows[i]; Product p=new Product(); p.name=r[0]; p.category=categories.get(r[1]); p.price=new BigDecimal(r[2]);
   p.materials=r[3]; p.district=r[4]; p.technique="Handmade craft tradition"; p.stock=10;
   p.dimensions="Approximate size; confirm with maker"; p.artisan=i%2==0?artisan:artisan2;
   p.imageUrl=imageFor(p.name,r[1]); p.description="A sample " + p.name + " listing from " + p.district + ". The image and maker details introduce the craft; please confirm exact materials and measurements with the artisan.";
   applyPhotoCredit(p); products.save(p);
  }
 }

 private String craftDescription(String category) {
  return switch (category) {
   case "Thanjavur Painting" -> "Traditional Thanjavur panels are prepared in layers, with relief ornament and fine brushwork creating a luminous surface. Keep framed work away from damp and direct sunlight.";
   case "Bronze Icons" -> "Metal icons are shaped through modelling, casting and careful finishing. Small changes in the mould and hand chasing make each maker's finish individual.";
   case "Brass Lamps" -> "Tamil Nadu lamp forms balance the stem, wick dish and foot. Turning, casting and polishing each contribute to the final profile and safe, stable use.";
   case "Clay & Pottery" -> "Potters prepare the clay, shape the walls, dry the form evenly and fire it. Handmade variations in shade, rim and profile are part of the process.";
   case "Kalamkari & Textiles" -> "Textile work is planned across a long surface; line, repeat, dye and border all need to stay in rhythm. Follow the maker's care instructions for dyes and fibre.";
   case "Palm Leaf & Jute" -> "Leaf and fibre strips are softened, interlaced and bound under even tension. Edge finishing and reinforced joins help a woven object keep its shape.";
   case "Wood Carving" -> "Carving begins with a planned outline and proceeds from broad cuts to fine detail, always working with the grain. Keep finished wood dry and away from heat.";
   case "Stone Carving" -> "Stone is taken from a rough block to a finished silhouette through measured chiselling and surface work. Confirm final weight and secure mounting before display.";
   default -> "This concept celebrates a hand-made tradition and the many decisions behind its finish. Confirm the exact process, material and care notes with the artisan.";
  };
 }

 private String canonicalCategory(String name) {
  return switch (name) {
   case "Thanjavur Painting" -> "Thanjavur Paintings";
   case "Bronze Icons" -> "Swamimalai Bronze";
   case "Brass Lamps" -> "Nachiarkoil Brass Lamps";
   case "Wood Carving" -> "Tamil Nadu Wood Carving";
   case "Stone Carving" -> "Mamallapuram Stone Carving";
   default -> name;
  };
 }

 private String imageFor(String name, String category) {
  String n=name.toLowerCase(Locale.ROOT), c=category.toLowerCase(Locale.ROOT);
  if(n.contains("art plate")) return "/images/commons/tamil-brass-lamp.jpg";
  if(n.contains("thanjavur")||n.contains("tanjore")||c.contains("painting")) return "/images/shiva-parvati-oval.png";
   if(n.contains("art plate")) return "/images/thanjavur-painting-process.png";
   if(c.contains("bronze")) return "/images/commons/swamimalai-bronze.jpg";
  if(c.contains("brass")) return "/images/commons/tamil-brass-lamp.jpg";
  if(c.contains("clay")||c.contains("pottery")) return "/images/potter-and-market.png";
  if(c.contains("kalamkari")||c.contains("textile")) return "/images/kalamkari-workshop.png";
  if(c.contains("palm")||c.contains("jute")) return "/images/commons/basket-weaving.jpg";
  if(c.contains("wood")) return "/images/commons/temple-wood-carving.jpg";
  if(c.contains("stone")) return "/images/wood-carver.png";
  return "/images/thanjavur-painting-process.png";
 }

 private void applyPhotoCredit(Product p) {
  String path=p.imageUrl==null?"":p.imageUrl;
  String file=path.substring(path.lastIndexOf('/')+1);
  p.imageCredit="Supplied by the project owner";
  p.imageSource="";
  if(path.contains("/images/commons/")) {
   Map<String,String[]> credits=Map.ofEntries(
    Map.entry("swamimalai-bronze.jpg",new String[]{"Psubhashish · CC BY-SA 4.0","https://commons.wikimedia.org/wiki/File:Swamimalai_bronze_icons_of_Tamilnadu.jpeg"}),
    Map.entry("palm-leaf-fan.jpg",new String[]{"Booradleyp1 · CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:Handmade_fan_-palm_leaf.jpg"}),
    Map.entry("palm-leaf-fan-wide.jpg",new String[]{"Booradleyp1 · CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:Handmade_fan-palm_leaf_1.jpg"}),
    Map.entry("thanjavur-doll.jpg",new String[]{"Thamizhpparithi Maari · CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:Thanjavur_Doll.JPG"}),
    Map.entry("tanjore-bobbing-doll.jpg",new String[]{"Suthir · CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:Tanjore_Thaliyatti_Pommai.jpg"}),
    Map.entry("tamil-brass-lamp.jpg",new String[]{"Darshiny · CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:Lamp-brass_-Tamil_Nadu.JPG"}),
    Map.entry("brass-light.jpg",new String[]{"Thamizhpparithi Maari · CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:A_brass_light.JPG"}),
    Map.entry("clay-pots.jpg",new String[]{"Raamleaks · CC BY-SA 4.0","https://commons.wikimedia.org/wiki/File:Clay_pots_and_vessels.jpg"}),
    Map.entry("temple-wood-carving.jpg",new String[]{"Tamil1510 · CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:Hand_work_in_wooden.jpg"}),
    Map.entry("basket-weaving.jpg",new String[]{"Sharuk.36 · CC BY-SA 4.0","https://commons.wikimedia.org/wiki/File:Yarcurd_Busketwaving.jpg"}),
    Map.entry("palm-leaf-weaving.jpg",new String[]{"Jayaseerlourdhuraj · CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:Olai_Pinnuthal.jpg"}),
    Map.entry("kanchipuram.jpg",new String[]{"McKay Savage · CC BY 2.0","https://commons.wikimedia.org/wiki/File:Silk_Sari_Weaving_at_Kanchipuram,_Tamil_Nadu.jpg"}),
    Map.entry("pottery.jpg",new String[]{"Thamizhpparithi Maari · CC BY-SA 3.0","https://commons.wikimedia.org/wiki/File:Art_of_Pot_Making_7.JPG"})
   );
   String[] credit=credits.get(file); if(credit!=null){p.imageCredit=credit[0];p.imageSource=credit[1];}
  }
 }
}

