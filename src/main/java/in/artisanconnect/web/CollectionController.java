package in.artisanconnect.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Keeps old bookmarked catalogue paths inside the unified TN ARTISAN marketplace. */
@Controller
public class CollectionController {
 @GetMapping({"/collection", "/collection/{id}"})
 String marketplace(@PathVariable(required=false) String id) { return "redirect:/products"; }
}
