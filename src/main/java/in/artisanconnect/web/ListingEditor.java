package in.artisanconnect.web;

import in.artisanconnect.domain.ArtisanProfile;
import in.artisanconnect.domain.Product;
import in.artisanconnect.repo.*;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Validated
public class ListingEditor {
 private final ProductRepository products;
 private final UserRepository users;
 private final CategoryRepository categories;
 private final ArtisanRepository artisans;
 private final Path photos=Path.of("data","listing-photos").toAbsolutePath();

 public ListingEditor(ProductRepository products, UserRepository users, CategoryRepository categories, ArtisanRepository artisans){
  this.products=products;
  this.users=users;
  this.categories=categories;
  this.artisans=artisans;
 }

 @GetMapping("/artisan-area/product/{id}/edit")
 String edit(@PathVariable Long id, Authentication auth, Model model){
  Product product=products.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  checkOwner(product,auth);
  model.addAttribute("product",product);
  model.addAttribute("categories",categories.findAll());
  return "listing-edit";
 }

 @Transactional
 @PostMapping("/artisan-area/product/{id}/edit")
 String save(@PathVariable Long id, Authentication auth, RedirectAttributes flash,
             @RequestParam @NotBlank @Size(max=160) String name,
             @RequestParam @NotBlank @Size(max=3000) String description,
             @RequestParam @Positive @Digits(integer=10,fraction=2) BigDecimal price,
             @RequestParam @Min(0) int stock, @RequestParam @Min(0) int originalStock,
             @RequestParam @Size(max=160) String materials, @RequestParam @Size(max=100) String technique,
             @RequestParam @Size(max=80) String district, @RequestParam @Size(max=180) String dimensions,
             @RequestParam Long categoryId, @RequestParam(defaultValue="false") boolean active,
             @RequestParam(required=false) MultipartFile photo){
  Product product=products.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  checkOwner(product,auth);
  String editPage="redirect:/artisan-area/product/"+id+"/edit";
  if(active && artisans.findByUserId(product.artisan.id).filter(p->p.status==ArtisanProfile.Verification.APPROVED).isEmpty()){
   flash.addFlashAttribute("error","Your artisan profile needs approval before publishing.");
   return editPage;
  }
  if(stock!=originalStock && product.stock!=originalStock){
   flash.addFlashAttribute("error","Stock changed while you were editing. Review the current count and try again.");
   return editPage;
  }
  var category=categories.findById(categoryId).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST));
  if(photo!=null && !photo.isEmpty()){
   try {
    product.imageUrl=storePhoto(photo);
    product.imageCredit="Provided by the artisan";
    product.imageSource=null;
   } catch(IOException ex){
    flash.addFlashAttribute("error","The photo could not be saved. Choose a JPEG or PNG under 5 MB and up to 4000 pixels per side.");
    return editPage;
   }
  }
  product.name=name.strip();
  product.description=description.strip();
  product.price=price;
  if(stock!=originalStock)product.stock=stock;
  product.materials=materials.strip();
  product.technique=technique.strip();
  product.district=district.strip();
  product.dimensions=dimensions.strip();
  product.category=category;
  product.active=active;
  flash.addFlashAttribute("success","Listing updated.");
  return editPage;
 }

 private void checkOwner(Product product, Authentication auth){
  if(!product.artisan.id.equals(users.findByEmailIgnoreCase(auth.getName()).orElseThrow().id))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN);
 }

 private String storePhoto(MultipartFile photo)throws IOException{
  try(var stream=ImageIO.createImageInputStream(photo.getInputStream())){
   if(stream==null)throw new IOException("Unreadable image");
   var readers=ImageIO.getImageReaders(stream);
   if(!readers.hasNext())throw new IOException("Unreadable image");
   var reader=readers.next();
   try{
    String format=reader.getFormatName();
    if(!format.equalsIgnoreCase("jpeg")&&!format.equalsIgnoreCase("png"))throw new IOException("Unsupported image");
    reader.setInput(stream);
    if(reader.getWidth(0)>4000||reader.getHeight(0)>4000)throw new IOException("Image too large");
    var image=reader.read(0);
    Files.createDirectories(photos);
    String file=UUID.randomUUID()+".png";
    Path target=photos.resolve(file);
    try {
     if(!ImageIO.write(image,"png",target.toFile()))throw new IOException("Could not write image");
    }catch(IOException ex){Files.deleteIfExists(target);throw ex;}
    return "/images/listing-uploads/"+file;
   }finally{reader.dispose();}
  }
 }

 @GetMapping("/images/listing-uploads/{file}")
 ResponseEntity<FileSystemResource> photo(@PathVariable String file){
  if(!file.matches("[0-9a-f-]{36}[.]png"))return ResponseEntity.notFound().build();
  Path path=photos.resolve(file);
  if(!Files.isRegularFile(path))return ResponseEntity.notFound().build();
  return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(new FileSystemResource(path));
 }
}
