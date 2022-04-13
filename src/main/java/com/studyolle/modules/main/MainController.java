package com.studyolle.modules.main;

import com.studyolle.modules.account.AccountRepository;
import com.studyolle.modules.account.CurrentAccount;
import com.studyolle.modules.account.Account;
import com.studyolle.modules.event.Enrollment;
import com.studyolle.modules.event.EnrollmentRepository;
import com.studyolle.modules.study.Study;
import com.studyolle.modules.study.StudyRepository;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.zone.Zone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final StudyRepository studyRepository;
    private final AccountRepository accountRepository;
    private final EnrollmentRepository enrollmentRepository;


    @GetMapping("/")
    public String home(@CurrentAccount Account account, Model model){
        if(account != null){
            Account accountLoaded = accountRepository.findAccountWithTagsAndZonesById(account.getId());

            List<Enrollment> enrollmentList =
                    enrollmentRepository.findByAccountAndAcceptedOrderByEnrolledAt(account, true);

            List<Study> studyList = studyRepository.findByAccount(accountLoaded.getZones(), accountLoaded.getTags());

            List<Study> studyManagerOf =
                    studyRepository.findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(account, false);
            List<Study> studyMemberOf =
                    studyRepository.findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(account, false);


            model.addAttribute("accountLoaded", accountLoaded);
            model.addAttribute("studyManagerOf", studyManagerOf);
            model.addAttribute("studyMemberOf", studyMemberOf);
            model.addAttribute("studyList", studyList);
            model.addAttribute("enrollmentList", enrollmentList);
            return "index-after-login";
        }

        model.addAttribute("studyList",
                studyRepository.findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(true, false));
        return "index";
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @GetMapping("/search/study")
    public String searchStudy(
            @PageableDefault(size = 9, sort = "publishedDateTime", direction = Sort.Direction.DESC) Pageable pageable,
            String keyword, Model model){

        Page<Study> studyPage = studyRepository.findByKeyword(keyword, pageable);

        model.addAttribute("studyPage", studyPage);
        model.addAttribute("keyword", keyword);

        return "search";
    }

}
