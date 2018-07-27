CREATE TABLE results (
  game_id VARCHAR(255) NOT NULL,
  round INT NOT NULL,
  date DATETIME NOT NULL,
  owner_id BIGINT NOT NULL,
  PRIMARY KEY (game_id, round),
  CONSTRAINT FK_owner_2_user_id
    FOREIGN KEY (owner_id)
    REFERENCES users (id)
    ON DELETE CASCADE
);

CREATE TABLE scores (
  id ${BIGINT_AUTO_INCREMENT},
  game_id VARCHAR(255) NOT NULL,
  round INT NOT NULL,
  player_id BIGINT NOT NULL,
  score INT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_game_2_game_id
    FOREIGN KEY (game_id, round)
    REFERENCES results (game_id, round)
    ON DELETE CASCADE,
  CONSTRAINT FK_player_2_user_id
    FOREIGN KEY (player_id)
    REFERENCES users (id)
    ON DELETE CASCADE
);

