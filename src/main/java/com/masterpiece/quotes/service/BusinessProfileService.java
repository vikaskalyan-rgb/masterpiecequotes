package com.masterpiece.quotes.service;

import com.masterpiece.quotes.dto.BusinessProfileDto;
import com.masterpiece.quotes.entity.BusinessProfile;
import com.masterpiece.quotes.exception.InvalidInputException;
import com.masterpiece.quotes.repository.BusinessProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class BusinessProfileService {

    private static final Set<Integer> ALLOWED_STEPS = Set.of(1, 10, 100, 500, 1000);

    private final BusinessProfileRepository repository;

    public BusinessProfileService(BusinessProfileRepository repository) {
        this.repository = repository;
    }

    // The row is created by migration V2; it is never missing in a migrated database.
    @Transactional(readOnly = true)
    public BusinessProfile current() {
        return repository.findById(BusinessProfile.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("business_profile row is missing - run migrations"));
    }

    @Transactional(readOnly = true)
    public BusinessProfileDto get() {
        return toDto(current());
    }

    @Transactional
    public BusinessProfileDto update(BusinessProfileDto dto) {
        if (!ALLOWED_STEPS.contains(dto.roundingStep())) {
            throw new InvalidInputException("roundingStep", "must be one of 1, 10, 100, 500 or 1000");
        }
        BusinessProfile p = current();
        p.setName(dto.name().trim());
        p.setTagline(blankToNull(dto.tagline()));
        p.setAddress(dto.address().trim());
        p.setPhonePrimary(dto.phonePrimary().trim());
        p.setPhoneSecondary(blankToNull(dto.phoneSecondary()));
        p.setEmail(blankToNull(dto.email()));
        p.setGstin(blankToNull(dto.gstin()));
        p.setSignatory(dto.signatory().trim());
        p.setRoundingStep(dto.roundingStep());
        p.setRoundingMode(dto.roundingMode());
        p.setValidityDays(dto.validityDays());
        p.setUpdatedAt(Instant.now());
        return toDto(repository.save(p));
    }

    public static BusinessProfileDto toDto(BusinessProfile p) {
        return new BusinessProfileDto(p.getName(), p.getTagline(), p.getAddress(), p.getPhonePrimary(),
                p.getPhoneSecondary(), p.getEmail(), p.getGstin(), p.getSignatory(), p.getRoundingStep(),
                p.getRoundingMode(), p.getValidityDays());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
