package com.studyolle.settings;

import com.studyolle.account.AccountService;
import com.studyolle.account.CurrentUser;
import com.studyolle.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final AccountService accountService;

    @GetMapping("/settings/profile")
    public String profileUpdateForm(@CurrentUser Account account, Model model){
        model.addAttribute(account);
        model.addAttribute(new Profile(account));
        return "settings/profile";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(@CurrentUser Account account, @Valid @ModelAttribute Profile profile, Errors errors,
                                Model model, RedirectAttributes attributes){
        if(errors.hasErrors()){
            model.addAttribute("account");
            return "settings/profile";
        }

        accountService.updateProfile(account, profile);
        attributes.addFlashAttribute("message", "프로필을 수정했습니다.");
        return "redirect:/settings/profile";
    }

}
