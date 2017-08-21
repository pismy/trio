-- ----------------------------------------------------------
-- Authority
-- ----------------------------------------------------------
insert into authorities (id, authority) values (1, 'ROLE_ADMIN');
insert into authorities (id, authority) values (2, 'ROLE_USER');

-- ----------------------------------------------------------
-- User
-- ----------------------------------------------------------
-- password 'admin'
insert into users (id, username, fullname, password, enabled, status) values (1, 'admin', 'Administrator', '$2a$10$jbqsQ21a4nSqgJjmfBblJ.e1PdfWhuj/Um557OY./alTnZoJROQyW', true, 1);

-- ----------------------------------------------------------
-- user_authorities
-- ----------------------------------------------------------
insert into user_authorities (user_id, authority_id) values (1, 1);
