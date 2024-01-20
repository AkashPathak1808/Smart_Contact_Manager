package com.smart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;

//	home page handler
	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("title", "Home");
		return "home";
	}

//	about page handler
	@GetMapping("/about")
	public String about(Model model) {
		model.addAttribute("title", "About");
		return "about";
	}

//	signup handler
	@GetMapping("/signup")
	public String signUp(Model model) {
		model.addAttribute("title", "Sign Up");
		model.addAttribute("user", new User());
		return "signup";
	}

//	post handler of register
	@PostMapping("/do_register")
	public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result,
			@RequestParam(value = "agreement", defaultValue = "false") boolean agreement, Model model,
			HttpSession session) {
		try {
			if (result.hasErrors()) {
				System.out.println(result);
				model.addAttribute("user", user);
				return "signup";
			}

			if (!agreement) {
				System.out.println("You have not agreed terms & conditions");
				throw new Exception("Accept Terms & Conditions");
			}

			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			user.setImageUrl("contact.png");

			System.out.println(agreement);
			System.out.println(user);

			this.userRepository.save(user);

			model.addAttribute("user", new User());

			session.setAttribute("message", new Message("Successfully Registered!! ", "alert-success"));

			return "signup";

		} catch (Exception e) {
			model.addAttribute("user", user);
			session.setAttribute("message", new Message("Something Went Wrong!! ", "alert-danger"));
			e.printStackTrace();
			return "signup";
		}
	}

//	login handler
	@GetMapping("/signin")
	public String customLogin(Model model) {
		model.addAttribute("title", "Login");
		return "login";
	}
	
//	logout handler
	@GetMapping("/logout")
	public String logOut(org.springframework.security.core.Authentication authentication, HttpServletRequest request,
			HttpServletResponse response) {
		SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
		logoutHandler.logout(request, response, authentication);
		return "login";
	}
}
