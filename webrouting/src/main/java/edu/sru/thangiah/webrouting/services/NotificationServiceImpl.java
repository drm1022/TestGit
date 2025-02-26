package edu.sru.thangiah.webrouting.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.sru.thangiah.webrouting.domain.Notification;
import edu.sru.thangiah.webrouting.domain.User;
import edu.sru.thangiah.webrouting.mailsending.Emailing;
import edu.sru.thangiah.webrouting.repository.NotificationRepository;

/**
 * Creates a notification service to assist with interactions with the notification service
 * @author Thomas Haley tjh1019@sru.edu
 * @since 1/01/2023
 */

@Service
public class NotificationServiceImpl implements NotificationService {

	@Autowired
	private Emailing emailImpl;

	private NotificationRepository notificationRepository;

	/**
	 * Constructor for the NotificationServiceImpl
	 * @param notificationRepository Instantiates the notification Repository
	 */
	public NotificationServiceImpl(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	/**
	 * Creates a new notification with the specified parameters
	 */

	@Override
	public void addNotification(User user, String message, Boolean toEmail) {
		LocalDateTime now = LocalDateTime.now();
		String time = now.toString();

		Notification notification = new Notification(user, time, message);

		if (toEmail) {
			emailImpl.notificationEmailFunction(notification.getMessage(), user.getEmail());
		}

		notificationRepository.save(notification);

	}

}
