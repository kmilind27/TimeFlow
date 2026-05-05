package com.company.leaveservice.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.company.leaveservice.entity.Holiday;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long>{
	
	@Query("SELECT h FROM Holiday h "
			+ "WHERE YEAR(h.holidayDate) = :year "
			+ "ORDER BY h.holidayDate ASC")
	List<Holiday> findByYear(@Param("year") int year);
	
	boolean existsByHolidayDate(LocalDate holidayDate);
	
	List<Holiday> findByHolidayDateBetween(LocalDate fromDate, LocalDate toDate);
	
	// For "Employee Summary"
	Optional<Holiday> findFirstByHolidayDateAfterOrderByHolidayDateAsc(LocalDate date);

}
