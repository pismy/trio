/*
 * Copyright (C) 2017 Orange
 *
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE.txt' in this package distribution
 * or at 'http://www.apache.org/licenses/LICENSE-2.0'.
 */
package com.orange.oswe.demo.trio.repository;

import com.orange.oswe.demo.trio.domain.Game;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository class for @{{@link Game}} domain objects
 */
public interface ResultRepository extends CrudRepository<Game, Game.Id> {
}
