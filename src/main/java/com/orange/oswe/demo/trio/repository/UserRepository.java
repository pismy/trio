/*
 * Copyright (C) 2017 Orange
 *
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE.txt' in this package distribution
 * or at 'http://www.apache.org/licenses/LICENSE-2.0'.
 */
package com.orange.oswe.demo.trio.repository;

import com.orange.oswe.demo.trio.domain.User;
import org.springframework.dao.DataAccessException;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository class for @{User} domain objects
 */
public interface UserRepository extends CrudRepository<User, Long> {

	/**
	 * Retrieve @{User} from the data store by username.
	 *
	 * @param username
	 *			Value to search for
	 * @return a <code>Collection</code> of matching @{User} (or an
	 *		 empty <code>Collection</code> if none found)
	 */
	User findByUsername(String username) throws DataAccessException;
}
