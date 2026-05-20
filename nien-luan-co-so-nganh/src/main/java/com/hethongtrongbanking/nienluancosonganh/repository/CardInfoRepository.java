package com.hethongtrongbanking.nienluancosonganh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hethongtrongbanking.nienluancosonganh.entity.CardInfo;

@Repository
public interface CardInfoRepository extends JpaRepository<CardInfo, String> {

}
