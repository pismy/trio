/*
 * Copyright (C) 2017 Orange
 *
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE.txt' in this package distribution
 * or at 'http://www.apache.org/licenses/LICENSE-2.0'.
 */
package com.orange.oswe.demo.trio.domain;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A system user.
 */
@Entity(name = "users")
@Data
public class User implements Serializable, UserDetails {

	private static final long serialVersionUID = 2002390446280945447L;

	private static final Logger logger = LoggerFactory.getLogger(User.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long id;

	@Column(unique = true)
	private String username;

	@Column
	private String fullname;

	@Column
	private String password;

	@Column
	private UserStatus status = UserStatus.WORKING;

	@Column
	private boolean enabled = true;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_authorities", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "authority_id"))
	private Set<Authority> authorities;

	public User() {
	}

	public User(String username, String fullname, String password) {
		this.username = username;
		this.fullname = fullname;
		this.password = password;
	}

	@Override
	public Set<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	public boolean hasAuthority(String targetAuthority) {
		if (targetAuthority == null) {
			return false;
		}
		if (authorities == null) {
			logger.warn("authorities is null for user " + this);
		}

		for (Authority authority : authorities) {
			if (targetAuthority.equals(authority.getAuthority())) {
				return true;
			}
		}

		return false;
	}

	public void addAuthority(Authority authority) {
		if (authority == null) {
			return;
		}
		if (authorities == null) {
			logger.warn("authorities is null for user " + this);
			authorities = new HashSet<Authority>();
		}

		authorities.add(authority);
	}

	@Override
	public boolean isAccountNonExpired() {
		return status != UserStatus.EXPIRED;
	}

	@Override
	public boolean isAccountNonLocked() {
		return status != UserStatus.LOCKED;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// never expire in our app
		return false;
	}
}
