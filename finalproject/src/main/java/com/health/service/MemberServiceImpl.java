package com.health.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.websocket.DecodeException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.health.dao.MemberDAO;
import com.health.dto.MemberDTO;

@Service
public class MemberServiceImpl implements MemberService {

	@Autowired
	MemberDAO memberdao;
	private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Override
	public UserDetails loadUserByUsername(String m_mail) throws UsernameNotFoundException { // principal 객체 - 로그아웃성공시
																							// 제거됨
		MemberDTO user = memberdao.readUser(m_mail);
		if (user == null) {
			throw new UsernameNotFoundException(m_mail);
		}
		user.setAuthorities(getAuthorities(m_mail));
		return user;
	}

	public Collection<GrantedAuthority> getAuthorities(String m_mail) {
		List<String> string_authorities = memberdao.readAuthority(m_mail);
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		for (String authority : string_authorities) {
			authorities.add(new SimpleGrantedAuthority(authority));
		}
		return authorities;
	}

	@Override
	public void createUser(MemberDTO dto) {
		String rawPassword = dto.getPassword();
		System.out.println("============================");
		System.out.println(rawPassword);
		System.out.println("============================");
		String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);
		dto.setPassword(encodedPassword);
		memberdao.createUser(dto);
		memberdao.createAuthority(dto);
	}

	@Override
	public void deleteUser(String m_mail) {
		memberdao.deleteUser(m_mail);
		memberdao.deleteAuthority(m_mail);
	}

	@Override
	public PasswordEncoder passwordEncoder() {
		return this.passwordEncoder;
	}

	@Override
	public MemberDTO getInfo(String m_mail) {
		return memberdao.readUser(m_mail);
	}

	// 이메일 중복체크
	public int checkEmail(String m_mail) {
		int result = memberdao.checkEmail(m_mail);
		return result;
	}
	

	@Override
	public boolean updateUser(Map<String, Object> param) {
		MemberDTO principal = (MemberDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();		
		
		if (!param.get("old_pw").equals("")) { //비밀번호 바뀔때
			String oldPassword = param.get("old_pw").toString();
			String rawPassword = param.get("m_pw").toString();
			String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);
			
			boolean chackpw = (boolean) passwordEncoder.matches(oldPassword, principal.getPassword());			
		
			if (chackpw) {
				principal.setPassword(encodedPassword);
				param.put("m_pw", encodedPassword);
			}else {
				return false;
			}

		}else {
			principal.setPassword(principal.getPassword());	//비밀번호가 유지 될때
			param.put("m_pw", principal.getPassword());
		}
		
		memberdao.updateMember(param);
		return true;
	}	

}