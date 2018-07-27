package com.orange.oswe.demo.trio.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

/**
 * The result of a user in a finished result.
 *
 * TODO: nb players, score, rank
 */
@Entity(name = "scores")
@Data
@ToString(of = {"player", "score"})
@NoArgsConstructor
@AllArgsConstructor(staticName = "build")
public class Score {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long id;

	@ManyToOne(optional = false)
	@JoinColumns({
			@JoinColumn(name = "game_id", referencedColumnName = "game_id"),
			@JoinColumn(name = "round", referencedColumnName = "round")
	})
	private Result result;

	@ManyToOne(optional = false)
	@JoinColumn(name = "player_id")
	private User player;

	@Column(nullable = false)
	private int score;
}
