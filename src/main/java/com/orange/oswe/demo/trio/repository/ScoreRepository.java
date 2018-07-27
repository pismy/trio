/*
 * Copyright (C) 2017 Orange
 *
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE.txt' in this package distribution
 * or at 'http://www.apache.org/licenses/LICENSE-2.0'.
 */
package com.orange.oswe.demo.trio.repository;

import com.orange.oswe.demo.trio.domain.Score;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository class for @{{@link Score}} domain objects
 */
public interface ScoreRepository extends CrudRepository<Score, Long> {
}
