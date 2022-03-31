package com.studyolle.event;

import com.studyolle.account.CurrentAccount;
import com.studyolle.domain.Account;
import com.studyolle.domain.Study;
import com.studyolle.event.form.EventForm;
import com.studyolle.study.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {

    private final StudyService studyService;

    @GetMapping("/new-event")
    public String newEventForm(@CurrentAccount Account account, @PathVariable String path, Model model){
        Study study = studyService.getStudyToUpdateStatus(account, path);

        model.addAttribute(account);
        model.addAttribute(study);
        model.addAttribute(new EventForm());

        return "event/form";
    }

}
