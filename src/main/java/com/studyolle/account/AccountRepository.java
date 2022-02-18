package com.studyolle.account;

import com.studyolle.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean findByEmail(String email);

    boolean findByNickname(String nickname);

}
