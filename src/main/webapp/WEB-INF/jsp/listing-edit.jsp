<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<section class="container section-pad narrow">
 <a href="/artisan-area">Back to seller dashboard</a>
 <h1 class="page-title mt-3">Edit listing</h1>
 <c:if test="${not empty error}"><p class="alert alert-warning"><c:out value="${error}"/></p></c:if>
 <c:if test="${not empty success}"><p class="alert alert-success"><c:out value="${success}"/></p></c:if>
 <form class="panel profile-form" method="post" enctype="multipart/form-data" action="/artisan-area/product/${product.id}/edit">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
  <input type="hidden" name="originalStock" value="${product.stock}">
  <img src="<c:out value='${product.imageUrl}'/>" alt="Current listing photo" width="120" height="120" style="object-fit:cover;border-radius:12px">
  <label>Replace photo <input class="form-control" type="file" name="photo" accept="image/jpeg,image/png"></label>
  <label>Product name <input class="form-control" name="name" maxlength="160" value="<c:out value='${product.name}'/>" required></label>
  <label>Description <textarea class="form-control" name="description" maxlength="3000" rows="5" required><c:out value="${product.description}"/></textarea></label>
  <div class="row"><label class="col">Price in rupees <input class="form-control" name="price" type="number" min=".01" step=".01" value="${product.price}" required></label>
  <label class="col">Available stock <input class="form-control" name="stock" type="number" min="0" value="${product.stock}" required></label></div>
  <label>Materials <input class="form-control" name="materials" maxlength="160" value="<c:out value='${product.materials}'/>"></label>
  <label>Technique <input class="form-control" name="technique" maxlength="100" value="<c:out value='${product.technique}'/>"></label>
  <label>District <input class="form-control" name="district" maxlength="80" value="<c:out value='${product.district}'/>"></label>
  <label>Dimensions <input class="form-control" name="dimensions" maxlength="180" value="<c:out value='${product.dimensions}'/>"></label>
  <label>Category <select class="form-select" name="categoryId"><c:forEach items="${categories}" var="category"><option value="${category.id}" ${category.id eq product.category.id ? 'selected' : ''}><c:out value="${category.name}"/></option></c:forEach></select></label>
  <label class="mt-3"><input type="checkbox" name="active" value="true" ${product.active ? 'checked' : ''}> Show in marketplace</label>
  <button class="btn btn-terracotta mt-3" type="submit">Save listing</button>
 </form>
</section>
<%@ include file="fragments/foot.jspf" %>
