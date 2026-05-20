package com.hethongtrongbanking.nienluancosonganh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hethongtrongbanking.nienluancosonganh.entity.FraudCase;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, Long> {

}
