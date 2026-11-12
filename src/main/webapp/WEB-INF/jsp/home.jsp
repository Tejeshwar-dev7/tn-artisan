<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="fragments/head.jspf" %>
<link rel="stylesheet" href="/css/landing.css">
<div class="craft-landing" id="top">
 <section class="landing-frame">
  <div class="landing-hero">
   <div class="landing-intro">
    <p class="landing-kicker hero-enter">THE CRAFT MARKETPLACE OF TAMIL NADU</p>
    <h1><span class="hero-enter">Crafted by</span><span class="hero-enter">tradition.</span><em class="hero-enter">Made to last.</em></h1>
    <p class="landing-lead hero-enter">Find handmade pieces from Tamil Nadu and get to know the makers and traditions behind them.</p>
    <div class="landing-actions"><a class="landing-button hero-enter" href="/products">Explore collection <span aria-hidden="true">&#8599;</span></a><a class="landing-button outline hero-enter" href="#makers">Meet the artisans</a></div>
   </div>
   <div class="landing-composition hero-enter">
    <div class="landing-orbit" aria-hidden="true"></div>
    <img class="landing-main-photo" src="/images/refresh/painting-closeup.png" alt="A brush adding detail to a gold Thanjavur painting" fetchpriority="high">
    <img class="landing-side-photo metal" src="/images/refresh/peacock-lamp.png" alt="A brass peacock lamp">
    <img class="landing-side-photo textile" src="/images/refresh/textile-detail.png" alt="Hands working on patterned cloth">
    <a class="landing-photo-caption" href="/about"><span><small>IN THE WORKSHOP</small><b>Thanjavur painting</b></span><span aria-hidden="true">&#8599;</span></a>
   </div>
  </div>
  <div class="landing-strip"><a href="#crafts">Browse by craft &#8595;</a><a href="#featured">Featured pieces &#8595;</a><a href="/cart">Basket <span>(<c:out value="${basketCount}"/>)</span></a></div>
 </section>

 <section class="landing-section" id="crafts">
  <header class="landing-heading" data-motion><p class="landing-kicker">BROWSE BY CRAFT</p><h2>Explore the art of Tamil Nadu.</h2></header>
  <div class="landing-categories">
   <c:forEach items="${categories}" var="category">
    <c:set var="cover" value=""/>
    <c:choose>
     <c:when test="${category.name eq 'Thanjavur Paintings'}"><c:set var="cover" value="/images/shiva-parvati-oval.png"/></c:when>
     <c:when test="${category.name eq 'Swamimalai Bronze'}"><c:set var="cover" value="/images/refresh/nandi-family.png"/></c:when>
     <c:when test="${category.name eq 'Tamil Nadu Wood Carving'}"><c:set var="cover" value="/images/commons/temple-wood-carving.jpg"/></c:when>
     <c:when test="${category.name eq 'Nachiarkoil Brass Lamps'}"><c:set var="cover" value="/images/refresh/annam-lamp.png"/></c:when>
     <c:when test="${category.name eq 'Mamallapuram Stone Carving'}"><c:set var="cover" value="/images/catalogue/stone-ganesha.jpg"/></c:when>
     <c:when test="${category.name eq 'Kalamkari & Textiles'}"><c:set var="cover" value="/images/refresh/cloth-painter.png"/></c:when>
    </c:choose>
    <c:if test="${not empty cover}"><a class="landing-category" data-motion href="/products?category=${category.id}"><img src="${cover}" alt="" loading="lazy"><span><c:out value="${category.name}"/> <b aria-hidden="true">&#8599;</b></span></a></c:if>
   </c:forEach>
  </div>
 </section>

 <section class="landing-section" id="featured">
  <header class="landing-heading split" data-motion><div><p class="landing-kicker">THE COLLECTION</p><h2>Handpicked creations.</h2></div><button class="landing-button outline" id="saved-filter" type="button" aria-pressed="false" hidden>Saved pieces</button><a href="/products">View all products &#8599;</a></header>
  <p id="saved-empty" hidden>No saved pieces in this collection yet.</p>
  <div class="landing-products">
   <c:forEach items="${products}" var="product">
    <article class="landing-product" data-motion>
     <a class="landing-product-image" href="/products/${product.id}"><img src="<c:out value='${product.imageUrl}'/>" alt="<c:out value='${product.name}'/>" loading="lazy"></a>
     <button class="landing-save" type="button" data-save="${product.id}" aria-label="Save product" aria-pressed="false" hidden>&#9825;</button>
     <h3><a href="/products/${product.id}"><c:out value="${product.name}"/></a></h3>
     <p><c:out value="${product.artisan.name}"/></p>
     <div class="landing-price"><b>&#8377;<fmt:formatNumber value="${product.price}" minFractionDigits="2" maxFractionDigits="2"/></b><c:if test="${not empty ratings[product.id]}"><span>&#9733; <fmt:formatNumber value="${ratings[product.id]}" maxFractionDigits="1"/></span></c:if></div>
    </article>
   </c:forEach>
  </div>
 </section>

 <section class="landing-section landing-story" id="makers">
  <div class="landing-story-image" data-motion><img src="/images/refresh/studio.png" alt="An artist working on a detailed drawing surrounded by framed work" loading="lazy"></div>
  <div data-motion><p class="landing-kicker">MEET THE ARTISANS</p><h2>Every creation has a story.</h2><p>See the care behind the finished piece, from the first brushstroke to the final polish. Maker profiles on product pages tell you who made each piece and where their craft comes from.</p><a class="landing-button" href="/about">Meet the makers &#8599;</a></div>
 </section>

 <section class="landing-section landing-heritage" data-motion>
  <p class="landing-kicker">OUR CRAFT HERITAGE</p><h2>Skills passed from hand to hand.</h2><p>Explore how makers work with gold relief, cast metal, carved wood and painted cloth. Each purchase gives a small workshop another way to reach customers.</p><a href="/about#our-why">Our purpose &#8599;</a>
 </section>
 <section class="landing-section">
  <header class="landing-heading" data-motion><h2>From the workshop to your home.</h2></header>
  <ol class="landing-steps"><li data-motion><b>Discover</b><span>Browse by craft or material.</span></li><li data-motion><b>Meet the maker</b><span>Read the maker’s story and product details.</span></li><li data-motion><b>Place your order</b><span>Check stock and choose how to pay.</span></li><li data-motion><b>Follow the delivery</b><span>See order updates in your account.</span></li></ol>
 </section>
 <section class="landing-section">
  <header class="landing-heading split" data-motion><h2>From our customers.</h2><a href="/reviews">All reviews &#8599;</a></header>
  <c:choose><c:when test="${empty recentReviews}"><p>No customer reviews yet.</p></c:when><c:otherwise><div class="landing-reviews"><c:forEach items="${recentReviews}" var="review"><article data-motion><b>&#9733; <c:out value="${review.rating}"/> / 5</b><blockquote><c:out value="${review.body}"/></blockquote><a href="/products/${review.product_id}"><c:out value="${review.product}"/></a></article></c:forEach></div></c:otherwise></c:choose>
 </section>
 <section class="landing-section landing-final" data-motion><h2>Bring home a piece of tradition.</h2><p>Find a handmade piece with a story you’ll want to share.</p><a class="landing-button" href="/products">Explore the collection &#8599;</a></section>
</div>
<script src="/js/landing.js" defer></script>
<%@ include file="fragments/foot.jspf" %>

