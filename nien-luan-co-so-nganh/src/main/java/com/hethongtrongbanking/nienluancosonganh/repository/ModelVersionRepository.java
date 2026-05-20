package com.hethongtrongbanking.nienluancosonganh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hethongtrongbanking.nienluancosonganh.entity.ModelVersion;

@Repository
public interface ModelVersionRepository extends JpaRepository<ModelVersion, Long> {

}
