CREATE TABLE product_reviews (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 product_id BIGINT NOT NULL REFERENCES products(id),
 buyer_id BIGINT NOT NULL REFERENCES users(id),
 rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
 review_text VARCHAR(1200) NOT NULL,
 UNIQUE (product_id, buyer_id)
);
ALTER TABLE orders ADD COLUMN payment_method VARCHAR(24) NOT NULL DEFAULT 'LEGACY_DEMO';
