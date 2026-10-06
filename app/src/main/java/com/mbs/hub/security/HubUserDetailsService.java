package com.mbs.hub.security;

import com.mbs.hub.core.member.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class HubUserDetailsService implements UserDetailsService {

    private final MemberRepository members;

    public HubUserDetailsService(MemberRepository members) { this.members = members; }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return members.findByEmail(email)
                .map(HubUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No member with email " + email));
    }
}
