package in.artisanconnect.web;
import in.artisanconnect.repo.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Controller
public class ReviewController {
    private final JdbcTemplate db;
    private final UserRepository users;
    private final ProductRepository products;
    public ReviewController(JdbcTemplate db, UserRepository users, ProductRepository products) {
        this.db=db; this.users=users; this.products=products;
    }
    public List<Map<String,Object>> forProduct(long id) {
        return db.queryForList("select rating, review_text as body from product_reviews where product_id=? order by id desc", id);
    }
    public boolean eligible(long id, Authentication auth) {
        if(auth==null || auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_CUSTOMER"))) return false;
        long buyer=users.findByEmailIgnoreCase(auth.getName()).orElseThrow().id;
        Long count=db.queryForObject("select count(*) from orders o join order_items i on i.order_id=o.id where o.customer_id=? and i.product_id=? and o.status='DELIVERED'",Long.class,buyer,id);
        return count!=null && count>0;
    }
    public record RatingSummary(double average, long count) {
        public double getAverage(){return average;}
        public long getCount(){return count;}
    }
    Map<Long,RatingSummary> summaries() {
        Map<Long,RatingSummary> ratings = new HashMap<>();
        db.query("select product_id, avg(rating * 1.0) as average_rating, count(*) as review_count from product_reviews group by product_id",
            rs -> { ratings.put(rs.getLong("product_id"), new RatingSummary(rs.getDouble("average_rating"), rs.getLong("review_count"))); });
        return ratings;
    }
    List<Map<String,Object>> recent(int limit) {
        return db.queryForList("select r.rating, r.review_text as body, p.name as product, p.id as product_id from product_reviews r join products p on p.id=r.product_id where p.active=true order by r.id desc limit ?", limit);
    }
    @GetMapping("/reviews")
    String reviews(Model model) {
        model.addAttribute("reviews", db.queryForList("select r.rating, r.review_text as body, p.name as product, p.id as product_id from product_reviews r join products p on p.id=r.product_id where p.active=true order by r.id desc limit 100"));
        return "reviews";
    }
    @PostMapping("/customer/reviews/{id}")
    String submit(@PathVariable long id, @RequestParam int rating, @RequestParam String body, Authentication auth) {
        if(rating<1 || rating>5 || body.strip().length()<10 || body.length()>1200) throw new ResponseStatusException(BAD_REQUEST);
        if(products.findById(id).filter(p->p.active).isEmpty()) throw new ResponseStatusException(NOT_FOUND);
        long buyer=users.findByEmailIgnoreCase(auth.getName()).orElseThrow().id;
        if(!eligible(id,auth)) throw new ResponseStatusException(FORBIDDEN,"Reviews require a delivered order.");
        try {
            db.update("insert into product_reviews(product_id,buyer_id,rating,review_text) values(?,?,?,?)",id,buyer,rating,body.strip());
        } catch(org.springframework.dao.DuplicateKeyException duplicate) {
            db.update("update product_reviews set rating=?,review_text=? where product_id=? and buyer_id=?",rating,body.strip(),id,buyer);
        }
        return "redirect:/products/"+id+"#reviews";
    }
}
