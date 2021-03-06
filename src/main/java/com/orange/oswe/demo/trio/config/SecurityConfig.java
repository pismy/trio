/*
 * Copyright (C) 2017 Orange
 *
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE.txt' in this package distribution
 * or at 'http://www.apache.org/licenses/LICENSE-2.0'.
 */
package com.orange.oswe.demo.trio.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;

/**
 * Class containing all security configuration and beans.
 */
@Configuration
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityConfig {

	/**
	 * Resources URL are excluded from Spring Security
	 * <p>
	 * All other APIs are unauthorized...
	 */
	private static final String[] UNSECURED_RESOURCE_LIST = new String[] { "/resources/**" };
	
	private static final String AUTHORITIES_BY_USERNAME = "select username, authority from user_authorities "
			+ "inner join users on user_authorities.user_id = users.id "
			+ "inner join authorities on user_authorities.authority_id = authorities.id " + "where username = ?";

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@Autowired
	public UserDetailsService userDetailsService(DataSource dataSource) {
		JdbcUserDetailsManager userDetailsService = new JdbcUserDetailsManager();
		userDetailsService.setDataSource(dataSource);
		userDetailsService.setAuthoritiesByUsernameQuery(AUTHORITIES_BY_USERNAME);
		return userDetailsService;
	}

	@Bean
	@Autowired
	public RememberMeServices rememberMeServices(UserDetailsService userDetailsService, @Value("${trio.rememberMe.key}") String rememberMeKey,
                                                 @Value("${trio.rememberMe.param}") String rememberMeParameter) {
		TokenBasedRememberMeServices rememberMeServices = new TokenBasedRememberMeServices(rememberMeKey, userDetailsService);
		rememberMeServices.setParameter(rememberMeParameter);
		return rememberMeServices;
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@Configuration
	protected static class ExternalAuthenticationSecurity extends GlobalAuthenticationConfigurerAdapter {
		@Autowired
		private DataSource dataSource;

		@Autowired
		private PasswordEncoder passwordEncoder;

		@Autowired
		private UserDetailsService userDetailsService;

		@Override
		public void init(AuthenticationManagerBuilder auth) throws Exception {
			//@formatter:off
			auth
				.userDetailsService(userDetailsService)
					.passwordEncoder(passwordEncoder)
				.and()
					.jdbcAuthentication()
						.authoritiesByUsernameQuery(AUTHORITIES_BY_USERNAME)
						.passwordEncoder(passwordEncoder)
						.dataSource(dataSource)
			;
			//@formatter:on
		}
	}

	@Configuration
	@Order(1)
	public static class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
		@Value("${trio.rememberMe.key}")
		private String rememberMeKey;

		@Autowired
        RememberMeServices rememberMeServices;

		@Override
		public void configure(WebSecurity web) throws Exception {
			//@formatter:off
			web
				.ignoring()
					.antMatchers(UNSECURED_RESOURCE_LIST);
			//@formatter:on
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			//@formatter:off
			http
				.csrf()
					// ignore our stomp endpoints since they are protected using Stomp headers
					.ignoringAntMatchers("/trio-websocket/**")
			.and()
				.headers()
					.frameOptions()
						.sameOrigin()
			.and()
				.authorizeRequests()
					// actuator endpoints need ADMIN role
					.antMatchers("/manage", "/manage/**")
						.hasRole("ADMIN")
					// all POST requests on /games need to be authenticated (create, join, actions)
					.antMatchers(HttpMethod.POST, "/games", "/games/**")
						.authenticated()
					// all the rest doesn't require any authentication
					.anyRequest()
						.permitAll()
			.and()
				.formLogin()
					.loginPage("/login")
					.loginProcessingUrl("/login")
					.permitAll()
			.and()
				.rememberMe()
					.useSecureCookie(true)
					.tokenValiditySeconds(60 * 60 * 24 * 10) // 10 days
					.rememberMeServices(rememberMeServices)
					.key(rememberMeKey)
			.and()
				.logout()
					.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
					.logoutSuccessUrl("/?logout")
			.and()
				.sessionManagement()
					.maximumSessions(1)
					.expiredUrl("/?expired");
			// @formatter:on
		}
	}
}
