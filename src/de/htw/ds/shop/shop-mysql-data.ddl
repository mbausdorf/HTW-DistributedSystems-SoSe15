SET CHARACTER SET utf8;
USE soap_shop;

SET FOREIGN_KEY_CHECKS=0;

TRUNCATE TABLE Customer;
TRUNCATE TABLE Article;
TRUNCATE TABLE Purchase;
TRUNCATE TABLE PurchaseItem;

SET FOREIGN_KEY_CHECKS=1;

INSERT INTO Customer VALUES (0, "ines", x'2dc899c8e59c150e3ecbb44c2b2d3550bacf41c036de624d3eb1f275120dd733', "Ines", "Bergmann", "Wiener Strasse 42", "10999", "Berlin", "ines.bergmann@web.de", "0172/2345678");
SET @c1 = LAST_INSERT_ID();
INSERT INTO Customer VALUES (0, "sascha", x'142193d5100dcc47ba7d8d46c375d718fbbeb6d50c1267b87674c936811116c6', "Sascha", "Baumeister", "Glogauer Strasse 17", "10999", "Berlin", "sascha.baumeister@gmail.com", "0174/3345975");
SET @c2 = LAST_INSERT_ID();

INSERT INTO Article VALUES (0, "CARIOCA Fahrrad-Schlauch, 28x1.5 Zoll", 167, 40);
SET @a1 = LAST_INSERT_ID();
INSERT INTO Article VALUES (0, "CONTINENTAL Fahrrad-Schlauch Tour, 28 Zoll", 336, 80);
SET @a2 = LAST_INSERT_ID();
INSERT INTO Article VALUES (0, "PROPHETE Fahrrad-Schlauch, 14x1.75 Zoll", 252, 20);
SET @a3 = LAST_INSERT_ID();

INSERT INTO Purchase VALUES (0, @c1, 1288605807761, 0.19);
SET @p1 = LAST_INSERT_ID();
INSERT INTO Purchase VALUES (0, @c2, 1288635807761, 0.19);
SET @p2 = LAST_INSERT_ID();

INSERT INTO PurchaseItem VALUES (0, @p1, @a1, 167, 2);
INSERT INTO PurchaseItem VALUES (0, @p2, @a2, 336, 1);
INSERT INTO PurchaseItem VALUES (0, @p2, @a3, 252, 4);