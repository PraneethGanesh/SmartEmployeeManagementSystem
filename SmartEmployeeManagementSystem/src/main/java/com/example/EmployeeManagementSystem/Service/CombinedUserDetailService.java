package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("combinedUserDetailService")
public class CombinedUserDetailService implements UserDetailsService {

    private final VendorRepo vendorRepository;
    private final EmployeeRepo employeeRepository;

    public CombinedUserDetailService(VendorRepo vendorRepository, EmployeeRepo employeeRepository) {
        this.vendorRepository = vendorRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Employee> employee = employeeRepository.findByEmail(username);
        if (employee.isPresent()) {
            return employee.get(); // must implement UserDetails
        }

        // Then try Vendor
        Optional<Vendor> vendor = vendorRepository.findByEmail(username);
        if (vendor.isPresent()) {
            return vendor.get();
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
