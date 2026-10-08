package com.myblog.user.service;

import com.myblog.user.MemberNames;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** MemberNames를 회원 표로 채운다. 한 번의 조회로 읽는다 (specs/005 T008). */
@Component
@Transactional(readOnly = true)
public class MemberNamesAdapter implements MemberNames {

    private final UserRepository users;

    public MemberNamesAdapter(UserRepository users) {
        this.users = users;
    }

    @Override
    public Map<Long, MemberName> namesOf(Collection<Long> memberIds) {
        Map<Long, MemberName> names = new HashMap<>();
        if (memberIds.isEmpty()) {
            return names;
        }
        for (User user : users.findAllById(memberIds)) {
            names.put(user.getId(), user.getDeletedAt() != null
                    ? MemberName.withdrawnMember()
                    : new MemberName(user.getId(), user.getNickname(), false));
        }
        return names;
    }
}
