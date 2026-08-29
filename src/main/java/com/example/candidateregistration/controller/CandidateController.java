package com.example.candidateregistration.controller;

import com.example.candidateregistration.dto.CandidateDTO;
import com.example.candidateregistration.model.Candidate;
import com.example.candidateregistration.service.CandidateService;
import com.example.candidateregistration.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateService candidateService;
    private final FileStorageService fileStorageService;

    public CandidateController(CandidateService candidateService, FileStorageService fileStorageService) {
        this.candidateService = candidateService;
        this.fileStorageService = fileStorageService;
    }

    // multipart/form-data with two parts:
    //   "candidate" -> JSON blob (application/json) matching CandidateDTO
    //   "resume"    -> the PDF/DOC/DOCX file
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateDTO> createCandidate(
            @RequestPart("candidate") @Valid CandidateDTO request,
            @RequestPart("resume") MultipartFile resumeFile) {
        CandidateDTO created = candidateService.createCandidate(request, resumeFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CandidateDTO>> getAllCandidates() {
        return ResponseEntity.ok(candidateService.getAllCandidates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidateDTO> getCandidateById(@PathVariable Long id) {
        return ResponseEntity.ok(candidateService.getCandidateById(id));
    }

    @GetMapping("/{id}/resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id) {
    	try {
    		System.out.println("resume downloading....");
            Candidate candidate = candidateService.getCandidateEntityById(id);
            Resource resource = fileStorageService.loadAsResource(candidate.getResumeStoredPath());

            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + candidate.getResumeFileName() + "\"")
                    .body(resource);	
    	}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    	
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable Long id) {
        candidateService.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }
}
