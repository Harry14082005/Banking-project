package com.hethongtrongbanking.nienluancosonganh.security;

import com.hethongtrongbanking.nienluancosonganh.entity.User;
import com.hethongtrongbanking.nienluancosonganh.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

//Concept: Spring Security se auto goi Class nay de tim kiem User trong DB
//Example: Khi user login -> Spring Secutiry goi loadUserByUsername() -> Tim user trong DB
// Tra ve user -> Spring Security tu dong so xanh password da hask

@Service
// Ke thua UserDetailsService de Spring biet user nay co quyen gi
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    // Ham tu dong goi khi nguoi dung co gang Login
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay user co ten: " + username));

        return user;
    }

}
