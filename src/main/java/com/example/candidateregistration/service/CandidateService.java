package com.example.candidateregistration.service;

import com.example.candidateregistration.dto.CandidateDTO;
import com.example.candidateregistration.dto.EducationDetailDTO;
import com.example.candidateregistration.dto.ExperienceDetailDTO;
import com.example.candidateregistration.exception.InvalidRequestException;
import com.example.candidateregistration.exception.ResourceNotFoundException;
import com.example.candidateregistration.model.Candidate;
import com.example.candidateregistration.model.CandidateType;
import com.example.candidateregistration.model.EducationDetail;
import com.example.candidateregistration.model.ExperienceDetail;
import com.example.candidateregistration.repository.CandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class CandidateService {

	private final CandidateRepository candidateRepository;
	private final FileStorageService fileStorageService;

	public CandidateService(CandidateRepository candidateRepository, FileStorageService fileStorageService) {
		this.candidateRepository = candidateRepository;
		this.fileStorageService = fileStorageService;
	}

	@Transactional
	public CandidateDTO createCandidate(CandidateDTO request, MultipartFile resumeFile) {
		validateExperienceRules(request);

		// Validate + save the file to disk first: if this fails, nothing is written to
		// the DB.
		String storedFilename = fileStorageService.storeResume(resumeFile);
		String originalFilename = StringUtils.cleanPath(
				resumeFile.getOriginalFilename() == null ? storedFilename : resumeFile.getOriginalFilename());

		Candidate candidate = Candidate.builder().fullName(request.getFullName()).email(request.getEmail())
				.mobile(request.getMobile()).candidateType(request.getCandidateType())
				.jobCategory(request.getJobCategory()).resumeFileName(originalFilename).resumeStoredPath(storedFilename)
				.referencedByName(normalizeOptional(request.getReferencedByName()))
				.referencedByNumber(normalizeOptional(request.getReferencedByNumber())).build();

		request.getEducationDetails().forEach(eduDto -> candidate.addEducationDetail(toEducationEntity(eduDto)));

		if (request.getCandidateType() == CandidateType.Experienced && request.getExperienceDetails() != null) {
			request.getExperienceDetails().forEach(expDto -> candidate.addExperienceDetail(toExperienceEntity(expDto)));
		}

		Candidate saved = candidateRepository.save(candidate);
		return toDTO(saved);
	}

	@Transactional(readOnly = true)
	public List<CandidateDTO> getAllCandidates() {
		return candidateRepository.findAll().stream().map(this::toDTO).toList();
	}

	@Transactional(readOnly = true)
	public CandidateDTO getCandidateById(Long id) {
		Candidate candidate = candidateRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
		return toDTO(candidate);
	}

	@Transactional
	public void deleteCandidate(Long id) {
		Candidate candidate = candidateRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
		candidateRepository.deleteById(id);
		fileStorageService.deleteIfExists(candidate.getResumeStoredPath());
	}

	@Transactional(readOnly = true)
	public Candidate getCandidateEntityById(Long id) {
		return candidateRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
	}

	// ---------- Validation ----------

	private void validateExperienceRules(CandidateDTO request) {
		if (request.getCandidateType() != CandidateType.Experienced) {
			return;
		}

		List<ExperienceDetailDTO> experiences = request.getExperienceDetails() == null ? Collections.emptyList()
				: request.getExperienceDetails();

		if (experiences.isEmpty()) {
			throw new InvalidRequestException("At least one experience record is required for Experienced candidates");
		}

		for (int i = 0; i < experiences.size(); i++) {
			ExperienceDetailDTO exp = experiences.get(i);
			String label = "Experience #" + (i + 1) + ": ";

			if (!exp.isCurrentlyWorking()) {
				if (exp.getToDate() == null || exp.getToDate().isBlank()) {
					throw new InvalidRequestException(label + "To Date is required when not currently working");
				}
				try {
					LocalDate from = LocalDate.parse(exp.getFromDate());
					LocalDate to = LocalDate.parse(exp.getToDate());
					if (from.isAfter(to)) {
						throw new InvalidRequestException(label + "From Date cannot be later than To Date");
					}
				} catch (java.time.format.DateTimeParseException e) {
					throw new InvalidRequestException(label + "From Date and To Date must be valid dates (yyyy-MM-dd)");
				}
			}
		}
	}

	// ---------- Mapping helpers ----------

	private EducationDetail toEducationEntity(EducationDetailDTO dto) {
		return EducationDetail.builder().qualification(dto.getQualification()).institutionName(dto.getInstitutionName())
				.boardOrUniversity(dto.getBoardOrUniversity()).yearOfPassing(dto.getYearOfPassing())
				.score(dto.getScore()).build();
	}

	private ExperienceDetail toExperienceEntity(ExperienceDetailDTO dto) {
		return ExperienceDetail.builder().companyName(dto.getCompanyName()).designation(dto.getDesignation())
				.fromDate(dto.getFromDate()).toDate(dto.isCurrentlyWorking() ? null : dto.getToDate())
				.currentlyWorking(dto.isCurrentlyWorking()).ctc(dto.getCtc()).build();
	}

	private CandidateDTO toDTO(Candidate candidate) {
		CandidateDTO dto = new CandidateDTO();
		dto.setId(candidate.getId());
		dto.setResumeFileName(candidate.getResumeFileName());
		dto.setFullName(candidate.getFullName());
		dto.setEmail(candidate.getEmail());
		dto.setMobile(candidate.getMobile());
		dto.setCandidateType(candidate.getCandidateType());
		dto.setJobCategory(candidate.getJobCategory());
		dto.setReferencedByName(candidate.getReferencedByName());
		dto.setReferencedByNumber(candidate.getReferencedByNumber());
		dto.setEducationDetails(candidate
				.getEducationDetails().stream().map(e -> new EducationDetailDTO(e.getQualification(),
						e.getInstitutionName(), e.getBoardOrUniversity(), e.getYearOfPassing(), e.getScore()))
				.toList());

		dto.setExperienceDetails(candidate
				.getExperienceDetails().stream().map(e -> new ExperienceDetailDTO(e.getCompanyName(),
						e.getDesignation(), e.getFromDate(), e.getToDate(), e.isCurrentlyWorking(), e.getCtc()))
				.toList());

		return dto;
	}

	private String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}
}
