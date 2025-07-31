package gift.service;

import gift.config.NotMatchPasswordException;
import gift.domain.AccountType;
import gift.domain.Member;
import gift.dto.CreateMemberRequest;
import gift.dto.CreateMemberResponse;
import gift.dto.LoginMemberRequest;
import gift.dto.UpdateMemberRequest;
import gift.dto.UpdateMemberResponse;
import gift.repository.MemberJpaRepository;
import gift.util.ShaUtil;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private final MemberJpaRepository memberRepository;

    public MemberService(MemberJpaRepository memberRepository) {
        this.memberRepository = memberRepository;
    }


    public CreateMemberResponse register(CreateMemberRequest request) {
        duplicateEmailCheck(request.email());
        String salt = ShaUtil.getSalt();
        String encryptPassword = ShaUtil.encrypt(request.password(), salt);
        Member member = memberRepository.save(new Member(null, request.email(), encryptPassword, salt, AccountType.LOCAL));
        return new CreateMemberResponse(member.getId(), member.getEmail());
    }

    public void login(LoginMemberRequest request) {
        Optional<Member> findMember = memberRepository.findByEmail(request.email());
        if (findMember.isEmpty()) {
            throw new NoSuchElementException("Email이 존재하지 않습니다.");
        }
        Member member = findMember.get();
        String requestPassword = ShaUtil.encrypt(request.password(), member.getSalt());
        if (!member.getPassword().equals(requestPassword)) {
            throw new NotMatchPasswordException("비밀번호가 일치하지 않습니다.");
        }
    }

    public List<Member> memberList() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        findByIdOrThrow(id);
        return memberRepository.findById(id).get();
    }

    public UpdateMemberResponse update(Long id, UpdateMemberRequest request) {
        findByIdOrThrow(id);
        duplicateEmailCheck(request.email());
        String salt = ShaUtil.getSalt();
        String encryptPassword = ShaUtil.encrypt(request.password(), salt);
        Member updateMember = memberRepository.findById(id)
                .map(member -> {
                    member.update(request.email(), encryptPassword, salt);
                    return member;
                }).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 멤버입니다."));

        return new UpdateMemberResponse(id, updateMember.getEmail(), updateMember.getPassword());
    }

    private void findByIdOrThrow(Long id) {
        Optional<Member> byEmail = memberRepository.findById(id);
        if (byEmail.isEmpty()) {
            throw new NoSuchElementException("존재하지 않는 멤버입니다.");
        }
    }

    private void duplicateEmailCheck(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new DuplicateKeyException("이미 존재하는 email입니다.");
        }
    }

    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
