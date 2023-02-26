package edu.sru.thangiah.webrouting.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.MultipartConfigElement;
import javax.swing.text.DateFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.AccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import edu.sru.thangiah.webrouting.utilities.BackupUtil;

@Controller
public class BackupController {
	
	@Value("${spring.datasource.username}")
	private String dbUsername;
	
	@Value("${spring.datasource.password}")
	private String dbPassword;
	
	private String dbName;
	private String outputFile;
	
	final static DateTimeFormatter CUSTOM_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_hh.mm");
	
	public BackupController() {
		this.dbName = "webrouting";
		this.outputFile = "";
	}
	
	@Scheduled(fixedRate = 1200000) //Every twenty minutes
	public void executeBackup() {
		try {
			LocalDateTime now = LocalDateTime.now();
			String dateString = now.format(CUSTOM_FORMATTER);
			System.out.println("backups/backup " + dateString + ".sql");
			BackupUtil.backupDatabase(dbUsername, dbPassword, dbName, "backups/backup" + dateString + ".sql");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/**
	 * Redirects users to the Database restore page
	 * @param model: stores the springboot model
	 * @return /database
	 */
	@GetMapping("/database")
	public String database(Model model) {
		return "database";
	}
	
	@PostMapping("/restore-database")
	public String restoreDatabase(Model model, @RequestParam("file") MultipartFile backupFile) throws AccessException, IOException {
		File tmpFile = convertMultipartFileToFile(backupFile);
		
		try {
			BackupUtil.restoreDatabase(dbUsername, dbPassword, dbName,tmpFile.getAbsolutePath(),model);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			model.addAttribute("message", "There was a problem loading the database! Please check your input file.");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			model.addAttribute("message", "There was a problem loading the database! Please check your input file.");
		}
		
		tmpFile.delete();
		
		return "database";
	}
	
	//Borrowed from https://stackoverflow.com/questions/29923682/how-does-one-specify-a-temp-directory-for-file-uploads-in-spring-boot
	//TODO: citation of code
	
	/**
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private File convertMultipartFileToFile(MultipartFile file) throws IOException
    {    
        File convFile = File.createTempFile("temp", ".xlsx"); // choose your own extension I guess? Filename accessible with convFile.getAbsolutePath()
        FileOutputStream fos = new FileOutputStream(convFile); 
        fos.write(file.getBytes());
        fos.close(); 
        return convFile;
    }
}
