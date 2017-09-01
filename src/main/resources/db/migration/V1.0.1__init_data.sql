-- ----------------------------------------------------------
-- Authority
-- ----------------------------------------------------------
INSERT INTO authorities (id, authority) VALUES (1, 'ROLE_ADMIN');
INSERT INTO authorities (id, authority) VALUES (2, 'ROLE_USER');

-- ----------------------------------------------------------
-- User
-- ----------------------------------------------------------
-- password 'admin'
INSERT INTO users (id, username, fullname, password, enabled, status) VALUES (1, 'admin', 'Administrator', '$2a$10$jbqsQ21a4nSqgJjmfBblJ.e1PdfWhuj/Um557OY./alTnZoJROQyW', true, 1);

-- ----------------------------------------------------------
-- user_authorities
-- ----------------------------------------------------------
INSERT INTO user_authorities (user_id, authority_id) VALUES (1, 1);
