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
-- password 'user'
insert into users (id, username, fullname, password, enabled, status) values (2, 'bpitt', 'Brad Pitt', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (3, 'wsmith', 'Will Smith', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (4, 'jdepp', 'Johnny Depp', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (5, 'ldicaprio', 'Leonardo Di Caprio', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (6, 'thanks', 'Tom Hanks', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (7, 'tcruise', 'Tom Cruise', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (8, 'hford', 'Harrisson Ford', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (9, 'gclooney', 'Georges Clooney', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (10, 'baffleck', 'Ben Affleck', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (11, 'jnicholson', 'Jasck Nicholson', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (12, 'rdeniro', 'Robert De Niro', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (13, 'apacino', 'Al Pacino', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);
insert into users (id, username, fullname, password, enabled, status) values (14, 'dhoffman', 'Dustin Hoffman', '$2a$10$j794jRcT4H0TNu8pO4ak1.mfZvQ50GGxgQFSkxCc/qnh3AuywoRf6', true, 1);

-- ----------------------------------------------------------
-- user_authorities
-- ----------------------------------------------------------
insert into user_authorities (user_id, authority_id) values (1, 1);
insert into user_authorities (user_id, authority_id) values (2, 2);
insert into user_authorities (user_id, authority_id) values (3, 2);
insert into user_authorities (user_id, authority_id) values (4, 2);
insert into user_authorities (user_id, authority_id) values (5, 2);
insert into user_authorities (user_id, authority_id) values (6, 2);
insert into user_authorities (user_id, authority_id) values (7, 2);
insert into user_authorities (user_id, authority_id) values (8, 2);
insert into user_authorities (user_id, authority_id) values (9, 2);
insert into user_authorities (user_id, authority_id) values (10, 2);
insert into user_authorities (user_id, authority_id) values (11, 2);
insert into user_authorities (user_id, authority_id) values (12, 2);
insert into user_authorities (user_id, authority_id) values (13, 2);
insert into user_authorities (user_id, authority_id) values (14, 2);

