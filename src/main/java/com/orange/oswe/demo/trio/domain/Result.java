/*
 * Copyright (C) 2017 Orange
 *
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE.txt' in this package distribution
 * or at 'http://www.apache.org/licenses/LICENSE-2.0'.
 */
package com.orange.oswe.demo.trio.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * A finished game result.
 */
@Entity(name = "results")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor(staticName = "build")
public class Result {

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "build")
    public static class Id implements Serializable {
        @Column(name = "game_id", nullable = false)
        String gameId;
        @Column(nullable = false)
        int round;
    }

    @EmbeddedId
    private Id id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
	private User owner;

	@Column
	private Date date;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "result", cascade = CascadeType.ALL)
	private Set<Score> scores;

}
