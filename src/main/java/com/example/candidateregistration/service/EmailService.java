package com.example.candidateregistration.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.Year;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.candidateregistration.dto.CandidateDTO;
import com.example.candidateregistration.model.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

	@Autowired
	private JavaMailSender mailSender;

	public void sendRegistrationSucessEmail(CandidateDTO candidateDto) {

		try {
			ClassPathResource resource = new ClassPathResource("templates/emails/candidate-registration-success.html");
			String body = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(candidateDto.getEmail());
			helper.setSubject("🎉 Registration Successful – Welcome to StarTech");
			String htmlBody = body.replace("${candidateName}", candidateDto.getFullName())
					.replace("${candidateEmail}", candidateDto.getEmail())
					.replace("${registrationId}", candidateDto.getId().toString())
					.replace("${registrationDate}", LocalDateTime.now().toString())
					.replace("${currentYear}", String.valueOf(Year.now().getValue()));

			helper.setText(htmlBody, true);

			mailSender.send(message);

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	public void sendAdminRegistrationEmail(User user) {
		try {
			System.out.println("sending email about admin");
			ClassPathResource resource = new ClassPathResource("templates/emails/admin-registration-template.html");
			String body = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);

			String approveUrl = "http://localhost:8080" + "/api/auth/admin/" + user.getId() + "/approve";
			String rejectUrl = "http://localhost:8080" + "/api/auth/admin/" + user.getId() + "/reject";

			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo("chenchu9618254711@gmail.com");
			helper.setSubject("🎉 Admin Registration Request - StarTech");
			String htmlBody = body.replace("${registration_id}", "ADMIN-" + user.getId())
					.replace("${registration-date}", LocalDateTime.now().toString())
					.replace("${admin-email}", user.getEmail()).replace(" ${admin-name}", user.getEmail())
					.replace("${rejectUrl}", rejectUrl).replace("${approveUrl}", approveUrl);

			System.out.println(htmlBody);
			helper.setText(htmlBody, true);

			mailSender.send(message);

		} catch (Exception e) {

			e.printStackTrace();
		}
	}
}