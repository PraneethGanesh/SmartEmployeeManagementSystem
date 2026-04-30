package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.HolidayDTO;
import com.example.EmployeeManagementSystem.Service.HolidayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/holidays")
public class HolidayController {
    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addHoliday(@RequestBody HolidayDTO dto) {
        return ResponseEntity.ok(holidayService.addHoliday(dto));
    }

    @GetMapping
    public ResponseEntity<?> getHolidays(
            @RequestParam(defaultValue = "0") int year
    ) {
        int targetYear = (year == 0) ? LocalDate.now().getYear() : year;
        return ResponseEntity.ok(holidayService.getHolidaysByYear(targetYear));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.ok("Holiday deleted");
    }
}