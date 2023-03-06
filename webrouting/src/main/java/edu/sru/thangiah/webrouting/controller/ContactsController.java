package edu.sru.thangiah.webrouting.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.sru.thangiah.webrouting.domain.Contacts;
import edu.sru.thangiah.webrouting.domain.Notification;
import edu.sru.thangiah.webrouting.domain.User;
import edu.sru.thangiah.webrouting.repository.ContactsRepository;
import edu.sru.thangiah.webrouting.services.SecurityService;
import edu.sru.thangiah.webrouting.services.UserService;
import edu.sru.thangiah.webrouting.web.UserValidator;

/**
 * Handles the Thymeleaf controls for the pages
 * dealing with contacts.
 * @author Ian Black		img1007@sru.edu
 * @author Logan Kirkwood	llk1005@sru.edu
 * @since 2/1/2022
 */

@Controller
public class ContactsController {
	
	@Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;
	
	private ContactsRepository contactsRepository;
	
	@Autowired
    private UserValidator userValidator;
	
	private static final Logger Logger = LoggerFactory.getLogger(ContactsController.class);
	
	
	/**
	 * Constructor for ContactsController. <br>
	 * Instantiates the contactsRepository
	 * @param contactsRepository Used to interact with Contacts in the database
	 */
	public ContactsController(ContactsRepository contactsRepository) {
		this.contactsRepository = contactsRepository;
	}
	
	/**
	 * Adds all of the contacts to the "contacts" model and redirects user to
	 * the contacts page.
	 * @param model Used to add data to the model
	 * @return "contacts"
	 */
	@RequestMapping({"/contacts"})
    public String showContactList(Model model, HttpSession session) {
		
		
		  try {
	            model.addAttribute("error",session.getAttribute("error"));
	        } catch(Exception e){
	            //do nothing
	        }
	        session.removeAttribute("error");
	        
		String redirectLocation = "/contacts";
		session.setAttribute("redirectLocation", redirectLocation);
		model.addAttribute("redirectLocation", redirectLocation);
		
        model.addAttribute("contacts", getLoggedInUser().getCarrier().getContacts());
        
        User user = getLoggedInUser();
        model = NotificationController.loadNotificationsIntoModel(user, model);
        
        return "contacts";
    }
    

	/**
	 * Redirects user to the /add/add-contact page
	 * @param model Used to add data to the model
	 * @param contact Stores information on the contact to be added
	 * @param result Ensures inputs from the user are valid
	 * @return "/add/add-contact"
	 */
  	@RequestMapping({"/signupcontact"})
      public String showContactSignUpForm(Model model, Contacts contact, BindingResult result, HttpSession session) {
  		
  		User user = getLoggedInUser();
        model = NotificationController.loadNotificationsIntoModel(user, model);
        model.addAttribute("redirectLocation", (String) session.getAttribute("redirectLocation"));
        
        return "/add/add-contact";
    }
      
  	/**
  	 * Adds a contact to the database. Checks if there are errors in the form. <br>
  	 * If there are no errors, the contact is saved in the contactsRepository. and the user is redirect to /contacts <br>
  	 * If there are errors, the user is redirected to the /add/add-contact page.
  	 * @param contacts Stores information on the contact to be added
  	 * @param result Checks user inputs to ensure they are valid
  	 * @param model Used to add data to the model
  	 * @return "redirect:/contacts" or "/add/add-contact"
  	 */
  	@RequestMapping({"/addcontact"})
  	public String addContact(@Validated Contacts contacts, BindingResult result, Model model, HttpSession session) {
  		userValidator.addition(contacts, result);
  		contacts.setCarrier(getLoggedInUser().getCarrier());
  		User user = getLoggedInUser();
        model = NotificationController.loadNotificationsIntoModel(user, model);
        String redirectLocation = (String) session.getAttribute("redirectLocation");
        model.addAttribute("redirectLocation", session.getAttribute("redirectLocation")); 
  		
  		if (result.hasErrors()) {
  			return "/add/add-contact";
		}
  		
  		Boolean deny = false;
  		List<Contacts> checkContacts = new ArrayList<>();
  		checkContacts = (List<Contacts>) contactsRepository.findAll();
  		
  		for(Contacts check: checkContacts) {
  			if(contacts.getEmailAddress().toString().equals(check.getEmailAddress().toString())) {
  				deny = true;
  				break;
  	  		}
  		}
  		
  		if(deny == true) {
  			model.addAttribute("error", "Unable to add Contact. Contact Email already in use");
  			Logger.error("{} attempted to add contact and it failed because the email address {} is already in use.", user.getUsername(), contacts.getEmailAddress().toString());
  			model.addAttribute("contacts", getLoggedInUser().getCarrier().getContacts());
  			
  			return "contacts";
			 
  		}
  		
  		contactsRepository.save(contacts);
  		Logger.info("{} successfully added a new contact with ID {}.", user.getUsername(), contacts.getId());
  		
  		return "redirect:" + redirectLocation;
  	}
  	
  	/**
     * Redirects user to the /uploadcontacts page when clicking "Upload an excel file" button in the contacts section of Carrier login
     * @param model used to add data to the model
     * @return "/uploadcontacts"
     */
    
    @RequestMapping({"/uploadcontacts"})
    public String showAddContactsExcel(Model model) {
 	   return "/uploadcontacts";
    }
  	
  	/**
  	 * Finds a contact using the id parameter and if found, redirects user to delete confirmation page
  	 * Checks if dependencies are empty before deleting it.
  	 * @param id Stores the ID of the contact to be deleted
  	 * @param model Used to add data to the model
  	 * @return "contacts" or "/delete/deletecontactconfirm"
  	 */
  	@GetMapping("/deletecontact/{id}")
    public String deleteContact(@PathVariable("id") long id, Model model, HttpSession session) {
        Contacts contacts = contactsRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Invalid contact Id:" + id));
        
        User user = getLoggedInUser();
        model = NotificationController.loadNotificationsIntoModel(user, model);
        
        if(!contacts.getDrivers().isEmpty() || !contacts.getTechnicians().isEmpty()) {
        	session.setAttribute("error", "Unable to delete due to dependency conflict."); 
        	Logger.error("{} attmpted to delete contact. Deletion failed due to dependency conflict.", user.getUsername());
        	model.addAttribute("contacts", getLoggedInUser().getCarrier().getContacts());
        	
        	return "redirect:" + (String) session.getAttribute("redirectLocation");
        }
        
        model.addAttribute("contacts", contacts);
        
    	return "/delete/deletecontactconfirm";
    }
  	
  	/**
  	 * Finds a contact using the id parameter and if found, deletes the contact and redirects to contacts page
  	 * @param id ID of the contact being deleted
  	 * @param model Used to add data to the model
  	 * @return "redirect:/contacts"
  	 */
  	@GetMapping("/deletecontactconfirmation/{id}")
    public String deleteContactConfirmation(@PathVariable("id") long id, Model model) {
  		Contacts contacts = contactsRepository.findById(id)
  	          .orElseThrow(() -> new IllegalArgumentException("Invalid contact Id:" + id));
  		
  		User user = getLoggedInUser();
        model = NotificationController.loadNotificationsIntoModel(user, model);
  		Logger.info("{} successfully deleted the contact with ID {}.", user.getUsername(), contacts.getId());
        contactsRepository.delete(contacts);
        return "redirect:/contacts";
    }
  	
  	/**
  	 * Finds a contact using the id parameter and if found, adds the details of that contact
  	 * to the view contact page
  	 * @param id Stores the ID of the contact to be viewed
  	 * @param model Used to add data to the model
  	 * @return "/view/view-contact"
  	 */
  	@GetMapping("/viewcontact/{id}")
    public String viewContact(@PathVariable("id") long id, Model model, HttpSession session) {
  		
  		model.addAttribute("redirectLocation", (String) session.getAttribute("redirectLocation"));
        Contacts contacts = contactsRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Invalid contact Id:" + id));
        
        model.addAttribute("contacts", contacts);
        
  		User user = getLoggedInUser();
        model = NotificationController.loadNotificationsIntoModel(user, model);
        
        return "contacts";
    }
  	
  	/**
  	 * Finds a contact using the id parameter and if found, adds the details of that contact
  	 * to a form and redirects the user to that update form.
  	 * @param id Stores the ID of the contact to be edited
  	 * @param model Used to add data to the model
  	 * @return "update/update-contact"
  	 */
  	@GetMapping("/editcontact/{id}")
    public String showEditForm(@PathVariable("id") long id, Model model, HttpSession session) {
        Contacts contacts = contactsRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Invalid contact Id:" + id));
        
        model.addAttribute("contacts", contacts);
        model.addAttribute("redirectLocation", (String) session.getAttribute("redirectLocation"));
  		User user = getLoggedInUser();
        model = NotificationController.loadNotificationsIntoModel(user, model);
        
        return "/update/update-contact";
    }
  	
  	/**
  	 * Updates a contact to the database. Checks if there are errors in the form. <br>
  	 * If there are no errors, the contact is updated in the contactsRepository. and the user is redirected to /contacts <br>
  	 * If there are errors, the user is redirected to the /update/update-contact page.
  	 * @param id Stores the ID of the contact to be updated
  	 * @param contact Stores information on the contact that will be updated
  	 * @param result Checks user inputs to ensure they are valid
  	 * @param model Used to add data to the model
  	 * @return "redirect:/contacts"
  	 */
  	@PostMapping("/updatecontact/{id}")
    public String updateContact(@PathVariable("id") long id, @Validated Contacts contact, 
      BindingResult result, Model model, HttpSession session) {
  		String redirectLocation = (String) session.getAttribute("redirectLocation");
  		model.addAttribute("redirectLocation", session.getAttribute("redirectLocation"));
  		
  		userValidator.addition(contact, result);
  		contact.setCarrier(getLoggedInUser().getCarrier());
  		User user = getLoggedInUser();
        model = NotificationController.loadNotificationsIntoModel(user, model);
  		
  		if (result.hasErrors()) {
            contact.setId(id);
            return "/update/update-contact";
        }
        
        Boolean deny = false;
  		List<Contacts> checkContacts = new ArrayList<>();
  		checkContacts = (List<Contacts>) contactsRepository.findAll();
  		
  		for(Contacts check: checkContacts) {
  			if(contact.getEmailAddress().toString().equals(check.getEmailAddress().toString()) && contact.getId() != check.getId()) {
  				deny = true;
  				break;
  	  		}
  		}
  		
  		if(deny == true) {
  			model.addAttribute("error", "Unable to add Contact. Contact Email already in use");
  			Logger.error("{} failed to add contact with email {} because the email is already in use.",user.getUsername(), contact.getEmailAddress());
  			model.addAttribute("contacts", getLoggedInUser().getCarrier().getContacts());
  			return "/contacts";
			 
  		}
            
        contactsRepository.save(contact);
        Logger.info("{} successfully updated the contact with ID {}.", user.getUsername(), contact.getId());
        return "redirect:"+ redirectLocation;
    }
  	
  	/**
	 * Returns the user that is currently logged into the system. <br>
	 * If there is no user logged in, null is returned.
	 * @return user2 or null
	 */
	public User getLoggedInUser() {
    	if (securityService.isAuthenticated()) {
    		org.springframework.security.core.userdetails.User user = 
    				(org.springframework.security.core.userdetails.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    		
    		User user2 = userService.findByUsername(user.getUsername());
    		
    		return user2;
    	}
    	else {
    		return null;
    	}
    }
}
