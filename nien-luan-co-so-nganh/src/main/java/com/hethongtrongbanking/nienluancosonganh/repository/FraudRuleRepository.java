package com.hethongtrongbanking.nienluancosonganh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hethongtrongbanking.nienluancosonganh.entity.FraudRule;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {

}
