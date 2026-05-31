
package com.hethongtrongbanking.nienluancosonganh.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // file cau hinh
@EnableWebSecurity

public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // May ma hoa mat khau (password encoder)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Cai dat Provider xac thuc (DaoAuthenticationProvider)
    // Noi nhan khai bao cho Spring Security biet:
    // Lay thong tin user tu customUserDetailsService + dung passwordEncoder de so
    // sanh
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService); // lay thong tin
        authProvider.setPasswordEncoder(passwordEncoder()); // so sanh password
        return authProvider;
    }

    // Ham nay final duyet: Khi goi API login, day username/password cho ham nay xu
    // ly va cap Token
    // AuthenticationConfiguration la noi gom tat ca lai: CustomUserDetails (vi
    // implements tu UserDetailsService)
    // + BcryptPassword + cac cau hinh khac
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // Ham quy dinh URL nao oke, URL nao bi cam
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Tat CSRF vi dang xai REST API tra ve JSON, khong dung Form HTML
                .csrf(csrf -> csrf.disable())

                // Tat CORS tam thoi (De test Postman cho de)
                // Sau nay lam frontend React(3000) -> bat lai de React goi API
                .cors(cors -> cors.disable())

                // Moi lan goi API ke tiep, phai trinh Token ra
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // bat dau phan quyen cho URL (Authorize HTTP Requests)
                .authorizeHttpRequests(auth -> auth

                        // Public: bat ki ai cung duoc Register & Login (khong can Token)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public tam thoi: De test Payment bang Postman khong can login
                        // (Sau nay se chuyen thanh .authenticated())
                        .requestMatchers("/api/payment/**").permitAll()

                        // Simulator: Chi ADMIN moi duoc bat/tat
                        // (Tam thoi permitAll de test, sau chuyen hasRole ADMIN)
                        .requestMatchers("/api/simulator/**").permitAll()

                        // Actuator health check
                        .requestMatchers("/actuator/**").permitAll()

                        // Admin: Chi user co Role la ADMIN (quan ly rule, xoa tai khoan)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Analyst: Role ADMIN va ANALYST deu duoc vao (de duyet FraudCase)
                        .requestMatchers("/api/analyst/**").hasAnyRole("ADMIN", "ANALYST")

                        // Tat ca cac API con lai deu phai co Token hop le (Role bat ky)
                        .anyRequest().authenticated()

                );

        // Dang ky Provider vao he thong
        // Xac nhan voi HttpSecurity: authenticationProvider() ton tai
        http.authenticationProvider(authenticationProvider());

        // Goi JwtAuthenticationFilter -> truoc khi cho phep request vao Controller
        // Filter kiem tra truc
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
