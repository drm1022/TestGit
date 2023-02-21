package edu.sru.thangiah.webrouting.controller;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.sru.thangiah.webrouting.domain.Bids;
import edu.sru.thangiah.webrouting.domain.Carriers;
import edu.sru.thangiah.webrouting.domain.Shipments;
import edu.sru.thangiah.webrouting.domain.User;
import edu.sru.thangiah.webrouting.repository.ShipmentsRepository;
import edu.sru.thangiah.webrouting.repository.UserRepository;
import edu.sru.thangiah.webrouting.services.SecurityService;
import edu.sru.thangiah.webrouting.services.UserService;

@Controller
public class AuctionController {
	
	@Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;
	
    /**
     * Constructor for AuctionController.
     */
	private ShipmentsRepository shipmentsRepository;
	private UserRepository userRepository;
	
	public AuctionController (ShipmentsRepository sr, UserRepository ur) {
		this.shipmentsRepository = sr;
		this.userRepository = ur;
	}
	
	/**
	 * Handles loading auctioning homepage
	 * @param model : used to add data to the page model
	 * @return : returns "auctioninghome"
	 */
	@RequestMapping("/auctioninghome")
	public String auctioningHome(Model model) {
		User user = getLoggedInUser();
		List<Shipments> allShipments = (List<Shipments>)shipmentsRepository.findAll();
		
		model.addAttribute("shipments", allShipments);
		
		return "auctioninghome";
	}
	
	/**
	 * Finds a given shipment by ID, then redirects to the force end auction confirmation page if the shipment has any bids placed on it
	 * If the shipment has no bids on it, or is not AVAILABLE SHIPMENT, returns the user to their page and alerts the master that the operation has failed
	 * @
	 */
	@RequestMapping("/forceendauction/{id}")
	public String forceEndAuction(@PathVariable("id") long id, Model model, HttpSession session) {
		Shipments shipment = shipmentsRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid shipment Id:" + id));
		
		User user = getLoggedInUser();
		String redirectLocation = (String) session.getAttribute("redirectLocation");
		List<Bids> bids = shipment.getBids(); 
		
		if (!user.getRole().toString().equals("MASTERLIST")) {
			System.out.println("Non-Master user attempeted to force end an auction!"); ///TODO: Replace this with a proper error message
			return "redirect:" + redirectLocation;
		}
		
		if (bids.size() < 1) {
			System.out.println("This shipment has no bids on it, cannot end an auction with no bids"); ///TODO: Replace this with an html pop in page if possible
			return "redirect:" + redirectLocation;
		}
		
		model.addAttribute("shipments", shipment);
        model.addAttribute("redirectLocation",redirectLocation);
        
		return "/force/forceendauctionconfirm";
	}
	/**
	 * Finds a given shipment by ID, then 
	 */
	@RequestMapping("/pushshipment/{id}")
	public String pushShipment(@PathVariable("id") long id, Model model, HttpSession session) {
		Shipments shipment = shipmentsRepository.findById(id)
        		.orElseThrow(() -> new IllegalArgumentException("Invalid shipment Id:" + id));
        User user = getLoggedInUser();
        String redirectLocation = (String) session.getAttribute("redirectLocation");
        
        if (!shipment.getFullFreightTerms().equals("PENDING")) {
        	System.out.println("Error: Non-pending shipment attempted to be moved to auction.");
        	return redirectLocation;
        }
        
        model.addAttribute("shipments", shipment);
        
		return "/push/pushshipmentconfirm";
	}
	
	/**
	 * 
	 */
	@RequestMapping("/pushshipmentconfirmation/{id}")
	public String pushShipmentConfirmation(@PathVariable("id") long id, Model model) {
		Shipments shipment = shipmentsRepository.findById(id)
	     .orElseThrow(() -> new IllegalArgumentException("Invalid Shipment Id:" + id));
		User user = getLoggedInUser();		
		
		if (user.getRole().toString().equals("CARRIER") || (user.getRole().toString().equals("SHIPPER") && !user.getShipments().contains(shipment))) {
			System.out.println("Error: Invalid permissions for pushing shipment");
			return "redirect:/pendingshipments";
		}
		
		if(shipment.getUser().getId() != user.getId()) {
			NotificationController.addNotification(shipment.getUser(), 
					"ALERT: Your shipment with ID " + shipment.getId() + " and Client " + shipment.getClient() + " was pushed to auction by " + user.getUsername());
		}
		
		shipment.setFullFreightTerms("AVAILABLE SHIPMENT");
		shipmentsRepository.save(shipment);
		
		return "redirect:/pendingshipments";
	}
	
	/**
	 * 
	 */
	@RequestMapping("/removefromauction/{id}")
	public String removeFromAuction(@PathVariable("id") long id, Model model, HttpSession session) {
		Shipments shipment = shipmentsRepository.findById(id)
			     .orElseThrow(() -> new IllegalArgumentException("Invalid Shipment Id:" + id));
		User user = getLoggedInUser();
		String redirectLocation = (String) session.getAttribute("redirectLocation");
		
		if (!user.getRole().toString().equals("MASTER") || !user.getRole().toString().equals("SHIPPER")) {
			System.out.println("User with invalid role " + user.getRole().toString() + " attempted to remove shipment from auction");
			return redirectLocation;
		}
		
		if(user.getRole().toString().equals("SHIPPER")) {
			if (shipment.getUser().getId() != user.getId()) {
				System.out.println("Shipper attempted to remove a shipment that wasn't their own from auction");
				return redirectLocation;
			}
		}
		
		model.addAttribute("shipments",shipment);
		return "/push/removefromauctionconfirm";
	}
	
	
	@RequestMapping("/removefromauctionconfirmation/{id}")
	public String removeFromAuctionConfirmation(@PathVariable("id") long id, Model model) {
		Shipments shipment = shipmentsRepository.findById(id)
	     .orElseThrow(() -> new IllegalArgumentException("Invalid Shipment Id:" + id));
		User user = getLoggedInUser();		
		
		if (user.getRole().toString().equals("CARRIER") || (user.getRole().toString().equals("SHIPPER") && !user.getShipments().contains(shipment))) {
			System.out.println("Error: Invalid permissions for pushing shipment");
			return "redirect:/pendingshipments";
		}
		
		if(shipment.getUser().getId() != user.getId()) {
			NotificationController.addNotification(shipment.getUser(), 
					"ALERT: Your shipment with ID " + shipment.getId() + " and Client " + shipment.getClient() + " was pushed to auction by " + user.getUsername());
		}
		
		shipment.setFullFreightTerms("AVAILABLE SHIPMENT");
		shipmentsRepository.save(shipment);
		
		return "redirect:/pendingshipments";
	}
	
	/**
	 * 
	 */
	@RequestMapping("/forceendauctionconfirmation/{id}")
	public String forceEndAuctionConfirmation(@PathVariable("id") long id, Model model, HttpSession session) {
		Shipments shipment = shipmentsRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid shipment Id:" + id));
		List<Bids> bids = shipment.getBids(); 
		String redirectLocation = (String) session.getAttribute("redirectLocation");
		User user = getLoggedInUser();
		User bidUser;
		Bids winningBid = null;
		double lowestBidValue = Double.POSITIVE_INFINITY; //set the current lowest bid to infinity so the first bid in the loop will become the new lowest, and then be tested against every other bid.
		
		try {
			//TODO: This currently cannot handle ties, it will just hand it off to whichever bid was placed later if there is a tie. A prompt should come up if there is a tie.
			for(Bids bid : bids) { //For each bid in shipment.bids 
				if (Integer.parseInt(bid.getPrice()) < lowestBidValue) {
					winningBid = bid; //TODO: For some reason this fails on bid values more than ten digits? not sure why. will investigate further. For now this function is too buggy to be considered working, so issue will remain open. 
					lowestBidValue = Double.parseDouble(bid.getPrice()); //TODO: (this should be added a seperate github issue) the price variable in Bids should not be a string, as we are now doing math on it. Should be a double
				}
			}
			
		} 
		catch (NumberFormatException e) {
			System.out.println("Caught an exception, invalid price format for bids. Does a shipment have a non numeric character in its bid price?"); //TODO: replace with proper error logging
			return "redirect:" + redirectLocation;
		}
		
		if (winningBid == null){
			System.out.println("Unable to select a winning bid"); //TODO: replace with proper error logging
			return "redirect:" + redirectLocation;
		}
		
		Carriers carrier = winningBid.getCarrier();
		
		shipment.setCarrier(carrier);
		shipment.setPaidAmount(winningBid.getPrice());
		shipment.setScac(carrier.getScac());
		shipment.setFullFreightTerms("BID ACCEPTED");
		
		if(shipment.getUser().getId() != user.getId()) {
			NotificationController.addNotification(shipment.getUser(), 
					"ALERT: Your shipment with ID " + shipment.getId() + " and Client " + shipment.getClient() + " had its auction forcefully ended. It was given to carrer" + shipment.getCarrier().getCarrierName());
			
		}
		
		bidUser = CarriersController.getUserFromCarrier(winningBid.getCarrier());
		NotificationController.addNotification(bidUser, 
				"ALERT: You have won the auction on shipment with ID " + shipment.getId() + " with a final bid value of " + winningBid.getPrice());
		
		shipmentsRepository.save(shipment);
		return "redirect:" + redirectLocation;
	}
	
	/**
	 * 
	 */
	@GetMapping("/toggleauctioning/{id}")
	public String toggleAuctioning(@PathVariable("id") long id, Model model, HttpSession session) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid shipment Id:" + id));
		User loggedinuser = getLoggedInUser();
		String redirectLocation = (String) session.getAttribute("redirectLocation");
		
		if(!loggedinuser.getRole().toString().equals("ADMIN")) {
			System.out.println("ERROR: Non admin user tried to toggle auctioning for another user!");
			return redirectLocation;
		}
		
		model.addAttribute("user",user);
		model.addAttribute("redirectLocation",redirectLocation);
		
		return "/toggle/toggleauctioningconfirm";
	}
	
	@GetMapping("/toggleauctioningconfirmation/{id}")
	public String toggleAuctioningconfirmation(@PathVariable("id") long id, Model model, HttpSession session) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid shipment Id:" + id));
		
		if (user.getAuctioningAllowed()) {
			user.setAuctioningAllowed(false);
			NotificationController.addNotification(user, 
					"ALERT: Your auctioning abilites have been disabled. Please contact your system Master to regain access.");
		}
		else {
			user.setAuctioningAllowed(true);
			NotificationController.addNotification(user, 
					"ALERT: Your auctioning abilites have been re-eneabled. Thank you!");
		}
		
		userRepository.save(user); 
		return "redirect:" + session.getAttribute("redirectLocation");
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
