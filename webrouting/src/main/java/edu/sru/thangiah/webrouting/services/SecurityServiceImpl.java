package edu.sru.thangiah.webrouting.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;


/**
 * Implements the SecurityService interface class <br>
 * Used as a service for Spring Security
 * @author Logan Kirkwood	llk1005@sru.edu
 * @since 1/30/2022
 */

@Service
public class SecurityServiceImpl implements SecurityService {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private UserDetailsService userDetailsService;

	private static final Logger Logger = LoggerFactory.getLogger(SecurityServiceImpl.class);

	/**
	 * Checks if user is authenticated using the Spring Authentication
	 * @return boolean - true if user is authenticated | false if user is not authenticated
	 */
	public boolean isAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || AnonymousAuthenticationToken.class.
				isAssignableFrom(authentication.getClass())) {
			return false;
		}
		return authentication.isAuthenticated();
	}

	/**
	 * Automatically logs the user in with the username and password string
	 * @param username Username of the user
	 * @param password Password of the user
	 */

	public void autoLogin(String username, String password) {
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
		System.out.println("Token Created");

		authenticationManager.authenticate(usernamePasswordAuthenticationToken);

		System.out.println(isAuthenticated());
		Logger.info("{} || attempted to login.", username);

		if (usernamePasswordAuthenticationToken.isAuthenticated()) {
			SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
			System.out.println("Auto Login Successful");
			Logger.info("{} || logged in successfully.", username);
		}
	}

}
