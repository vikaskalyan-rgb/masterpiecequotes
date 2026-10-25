package com.masterpiece.quotes.repository;

import com.masterpiece.quotes.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Short> {
}
