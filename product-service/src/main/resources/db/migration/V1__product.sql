CREATE TABLE categories(id UUID PRIMARY KEY,name VARCHAR(120) UNIQUE NOT NULL);
CREATE TABLE products(id UUID PRIMARY KEY,sku VARCHAR(80) UNIQUE NOT NULL,name VARCHAR(200) NOT NULL,description TEXT NOT NULL,price NUMERIC(12,2) NOT NULL CHECK(price>=0),category_id UUID REFERENCES categories(id),active BOOLEAN NOT NULL DEFAULT TRUE,updated_at TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_product_name ON products(lower(name));CREATE INDEX idx_product_category ON products(category_id);INSERT INTO categories(id,name) VALUES('00000000-0000-0000-0000-000000000001','General');
