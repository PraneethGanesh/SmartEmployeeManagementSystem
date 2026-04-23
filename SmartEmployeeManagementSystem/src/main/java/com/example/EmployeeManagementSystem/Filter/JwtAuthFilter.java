package com.example.EmployeeManagementSystem.Filter;

import com.example.EmployeeManagementSystem.Util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Autowired
    @Qualifier("combinedUserDetailService")
    private UserDetailsService customUserDetailService;
    @Autowired
    private JWTUtil jwtUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token=null;
        String username=null;

        String authHeader=request.getHeader("Authorization");
        try {

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                username = jwtUtil.extractUsernameFromToken(token);
            }

        } catch (io.jsonwebtoken.ExpiredJwtException e) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write("""
        {
          "error": "TOKEN_EXPIRED",
          "message": "JWT expired. Please login again to get a new token."
        }
        """);

            return;
        }
        if (username!=null && SecurityContextHolder.getContext().getAuthentication()==null){
             UserDetails userDetails=customUserDetailService.loadUserByUsername(username);
             if(userDetails!=null && jwtUtil.validateToken(username,userDetails,token)){
                 UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(
                         userDetails,null,userDetails.getAuthorities()
                 );
                 SecurityContextHolder.getContext().setAuthentication(authenticationToken);
             }
        }

   filterChain.doFilter(request,response);


    }

}
