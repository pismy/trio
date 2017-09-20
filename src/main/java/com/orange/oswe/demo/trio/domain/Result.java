package com.orange.oswe.demo.trio.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * The result of a user in a finished game.
 *
 * TODO: nb players, score, rank
 */
@Entity(name = "results")
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "build")
public class Result {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long id;

	@ManyToOne(optional = false)
	@JoinColumns({
			@JoinColumn(name = "game_id", referencedColumnName = "game_id"),
			@JoinColumn(name = "round", referencedColumnName = "round")
	})
	private Game game;

	@ManyToOne(optional = false)
	@JoinColumn(name = "player_id")
	private User player;

	@Column(nullable = false)
	private int score;
}
