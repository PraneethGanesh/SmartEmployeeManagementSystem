package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.HolidayDTO;
import com.example.EmployeeManagementSystem.Entity.FixedHoliday;
import com.example.EmployeeManagementSystem.Repository.FixedHolidayRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
@Service
public class HolidayService {
    private final FixedHolidayRepository holidayRepository;

    public HolidayService(FixedHolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;

    }

    // Admin adds a holiday
    public FixedHoliday addHoliday(HolidayDTO dto) {
        FixedHoliday h = new FixedHoliday();
        h.setName(dto.getName());
        h.setDate(LocalDate.parse(dto.getDate()));
        h.setDescription(dto.getDescription());
        h.setYear(LocalDate.parse(dto.getDate()).getYear());
        return holidayRepository.save(h);
    }

    // Everyone can view holidays
    public List<FixedHoliday> getHolidaysByYear(int year) {
        return holidayRepository.findByYear(year);
    }

    // Admin deletes a holiday
    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }
}
