package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@ModelAttribute
	public void addCommanData(Model model, Principal principal) {
		String username = principal.getName();
		System.out.println(username);
		User user = this.userRepository.getUserByUserName(username);
		System.out.println(user);

		model.addAttribute("user", user);
	}

//	show dashboard
	@GetMapping("/index")
	public String dashboard(Model model) {
		model.addAttribute("title", "User_Dashboard");
		return "normal/user_dashboard";
	}

//	add contact handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add-Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

//	adding contact into database
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute("contact") Contact contact, BindingResult result,
			@RequestParam("image") MultipartFile file, Principal principal, Model model, HttpSession session) {
		try {
			model.addAttribute("title", "Add-Contact");

			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			if (file.isEmpty()) {
				System.out.println("File is empty");
				contact.setImage("contact.png");
			} else {
				contact.setImage(file.getOriginalFilename());

				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("Img uploaded");
			}

			contact.setUser(user);
			user.getContacts().add(contact);

			this.userRepository.save(user);
			System.out.println("Added");

			session.setAttribute("message", new Message("Your Contact Added!! Add More....", "success"));

		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something Went Wrong!! Try Again....", "danger"));
		}
		return "normal/add_contact_form";
	}

//	show all contacts
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "View-Contact");

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);

		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

//	show any particular contact
	@GetMapping("/{cId}/contact")
	public String showContact(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		model.addAttribute("title", "Show-Contact");
		System.out.println(cId);

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
		}

		return "normal/contact_detail";
	}

//	delete contact
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Principal principal, HttpSession session) {
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			user.getContacts().remove(contact);
			this.userRepository.save(user);
			session.setAttribute("message", new Message("Contact Deleted Successfully!!!", "success"));
		}
		return "redirect:/user/show-contacts/0";
	}

//	update any contact
	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		model.addAttribute("title", "Update-Contact");

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
		}

		return "normal/update_form";
	}

//	update form handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, BindingResult result,
			@RequestParam("image") MultipartFile file, Model model, HttpSession session, Principal principal) {

		try {
//			old photo
			Contact oldContactDetails = this.contactRepository.findById(contact.getcId()).get();
			if (!file.isEmpty()) {
//				delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldContactDetails.getImage());
				file1.delete();

//				update new Photo
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			} else {
				contact.setImage(oldContactDetails.getImage());
			}
			String userName = principal.getName();
			User user = this.userRepository.getUserByUserName(userName);
			contact.setUser(user);
			this.contactRepository.save(contact);

			session.setAttribute("message", new Message("Your Contact is Updated!!", "success"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("contact name: " + contact.getName());
		System.out.println("contact id: " + contact.getcId());

		return "redirect:/user/" + contact.getcId() + "/contact";
	}

//	profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile");
		return "normal/profile";
	}

//	setting handler
	@GetMapping("/setting")
	public String openSetting(Model model) {
		model.addAttribute("title", "Setting");
		return "normal/setting";
	}

//	change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {
		System.out.println(oldPassword);
		System.out.println(newPassword);

		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);

		if (this.passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
			currentUser.setPassword(this.passwordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);

			session.setAttribute("message", new Message("Successfully Changed!!", "success"));

			return "redirect:/user/index";
		} else {
			session.setAttribute("message", new Message("Please Enter Your Correct Old Password!", "danger"));
			return "redirect:/user/setting";
		}
	}
}
