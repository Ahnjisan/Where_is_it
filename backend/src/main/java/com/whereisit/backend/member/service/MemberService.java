package com.whereisit.backend.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.auth.error.AuthErrorCode;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.global.error.CommonErrorCode;
import com.whereisit.backend.member.dto.MemberResponse;
import com.whereisit.backend.member.dto.UpdateMemberRequest;
import com.whereisit.backend.member.entity.LanguageCode;
import com.whereisit.backend.member.entity.Member;
import com.whereisit.backend.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;

	/** API-03. AT는 유효한데 회원이 없으면(삭제 등) 그 토큰을 무효로 본다. */
	@Transactional(readOnly = true)
	public MemberResponse getMe(Long memberId) {
		return memberRepository.findById(memberId)
				.map(MemberResponse::from)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));
	}

	/** API-20. 회원 확인은 API-03과 같고, 같은 언어로 다시 요청해도 200이다. */
	@Transactional
	public MemberResponse updateMe(Long memberId, UpdateMemberRequest request) {
		LanguageCode languageCode = LanguageCode.fromCode(request.languageCode())
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNSUPPORTED_LANGUAGE));
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));

		member.changeLanguageCode(languageCode);
		return MemberResponse.from(member);
	}
}
