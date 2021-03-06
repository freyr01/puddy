package com.paymybuddy.puddy.controller;

import java.security.Principal;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.paymybuddy.puddy.enums.CURRENCY;
import com.paymybuddy.puddy.exceptions.AlreadyExistContactException;
import com.paymybuddy.puddy.exceptions.EmailAlreadyExistsException;
import com.paymybuddy.puddy.exceptions.InvalidArgumentException;
import com.paymybuddy.puddy.exceptions.NotEnoughCreditException;
import com.paymybuddy.puddy.exceptions.PasswordNotMatchException;
import com.paymybuddy.puddy.form.UserForm;
import com.paymybuddy.puddy.model.Transfer;
import com.paymybuddy.puddy.model.User;
import com.paymybuddy.puddy.service.ITransferService;
import com.paymybuddy.puddy.service.IUserService;

@Controller
public class MainController {
	
	private static Logger log = LoggerFactory.getLogger(MainController.class);
	
	private IUserService userService;
	private ITransferService transferService;
	
	@Autowired
	public MainController(IUserService p_userService, ITransferService p_transferService) {
		userService = p_userService;
		transferService = p_transferService;
	}
	
	@GetMapping(value="/login")
	public String login(@RequestParam(required = false) Object error, Model model) {
		if(error != null) {
			log.warn("User failed to login");
			model.addAttribute("error", "error");
		}
		return "login";
	}
	
	@GetMapping(value="/")
	public String root() {
		return "redirect:/home";
	}
	
	@GetMapping(value="/home")
	public String home(Principal principal, Model model) {
		User user = userService.getUserByMail(principal.getName());
		model.addAttribute("user", user);
		return "home";
	}
	
	@GetMapping(value="/transfer")
	public String transfer(Principal principal, Model model, @RequestParam(defaultValue = "0") int page) {
		User user = userService.getUserByMail(principal.getName());
		Page<Transfer> pages = transferService.getTransferOfUser(principal.getName(), page);
		model.addAttribute("user", user);
		model.addAttribute("transfers", pages);
		model.addAttribute("transferTotalPages", pages.getTotalPages());
		model.addAttribute("location", " / Transfer");
		return "transfer";
	}
	
	@PostMapping(value="/transfer")
	public String doTransfer(Principal principal, Model model,
			@RequestParam("connections") String connections,
			@RequestParam("amount") String amount,
			@RequestParam("description") String description) {
		log.info("POST Request on /transfer with params: connections: {}, amount: {}, description: {}", connections, amount, description);
		
		try {
			transferService.doTransfer(principal.getName(), connections, amount, CURRENCY.EUR, description);
		} catch (NotEnoughCreditException | InvalidArgumentException e) {
			log.error(e.getMessage());
			model.addAttribute("error", e.getMessage());
		}
		
		return transfer(principal, model, 0);
	}
	
	@GetMapping(value="/profile")
	public String profile(Principal principal, UserForm userForm, Model model) {
		User user = userService.getUserByMail(principal.getName());
		model.addAttribute("user", user);
		model.addAttribute("location", " / Profile");
		return "profile";
	}
	
	@PostMapping(value = "/profile")
	public String postProfile(@Valid UserForm userForm, BindingResult bindingResult, Principal principal, Model model) {
		model.addAttribute("userForm", userForm);
		User user = userService.getUserByMail(principal.getName());
		model.addAttribute("user", user);
		model.addAttribute("location", " / Profile");
		if(bindingResult.hasErrors()) {
			
			return "profile";
		}
		else
		{
			try {
				userService.editUser(userForm, user);
			} catch (PasswordNotMatchException e) {
				bindingResult.addError(new FieldError("password_confirm", "password_confirm", e.getMessage()));
				return "profile";
			} catch (EmailAlreadyExistsException e) {
				bindingResult.addError(new FieldError("email", "email", e.getMessage()));
				return "profile";
			}
			return "redirect:/home";
		}
	}
	
	@GetMapping(value="/register")
	public String register(UserForm userForm) {
		return "register";
	}
	@PostMapping(value="/register")
	public String submitRegister(@Valid UserForm userForm, BindingResult bindingResult, Model model) {
		
			model.addAttribute("userForm", userForm);
		if(bindingResult.hasErrors()) {
			
			return "register";
		}
		else
		{
			try {
				userService.addNewUser(userForm);
			} catch (PasswordNotMatchException e) {
				bindingResult.addError(new FieldError("password_confirm", "password_confirm", e.getMessage()));
				return "register";
			} catch (EmailAlreadyExistsException e) {
				bindingResult.addError(new FieldError("email", "email", e.getMessage()));
				return "register";
			}
			return "register_success";
		}
	}
	
	@GetMapping(value="/contact")
	public String contact(Principal principal, Model model) {
		User user = userService.getUserByMail(principal.getName());
		model.addAttribute("user", user);
		model.addAttribute("location", " / Contact");
		return "contact";
	}
	
	@PostMapping(value="/contact")
	public String postContact(Principal principal, Model model, @RequestParam("email") String email) {
		try {
			userService.addContact(principal.getName(), email);
		} catch (AlreadyExistContactException | UsernameNotFoundException e) {
			log.error(e.getMessage());
			model.addAttribute("error", e.getMessage());
		}
		return contact(principal, model);
	}
}
