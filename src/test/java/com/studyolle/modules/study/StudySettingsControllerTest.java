package com.studyolle.modules.study;

import com.studyolle.infra.MockMvcTest;
import com.studyolle.modules.account.AccountFactory;
import com.studyolle.modules.account.AccountRepository;
import com.studyolle.modules.account.AccountService;
import com.studyolle.modules.account.form.SignUpForm;
import com.studyolle.modules.account.Account;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@MockMvcTest
class StudySettingsControllerTest {

    @Autowired AccountService accountService;
    @Autowired AccountRepository accountRepository;
    @Autowired AccountFactory accountFactory;
    @Autowired StudyFactory studyFactory;
    @Autowired MockMvc mockMvc;

    @BeforeEach
    public void beforeEach(){
        SignUpForm signUpForm = new SignUpForm();
        signUpForm.setEmail("zering@test.com");
        signUpForm.setNickname("zering");
        signUpForm.setPassword("123456789");
        accountService.processNewAccount(signUpForm);
    }

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }


    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("스터디 소개 수정 폼 조회 - 실패 (권한 없는 유저)")
    @Test
    public void updateDescriptionForm_fail() throws Exception {
        Account testAccount = accountFactory.createAccount("testAccount");
        Study study = studyFactory.createStudy("test-study", testAccount);

        mockMvc.perform(get("/study/" + study.getPath() + "/settings/description"))
                .andExpect(status().isForbidden());
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("스터디 소개 수정 폼 조회 - 성공")
    @Test
    public void updateDescriptionForm_success() throws Exception {
        Account zering = accountRepository.findByNickname("zering");
        Study study = studyFactory.createStudy("test-study", zering);

        mockMvc.perform(get("/study/" + study.getPath() + "/settings/description"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/settings/description"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("studyDescriptionForm"));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("스터디 소개 수정 - 성공")
    @Test
    public void updateDescription_success() throws Exception {
        Account zering = accountRepository.findByNickname("zering");
        Study study = studyFactory.createStudy("test-study", zering);

        String url = "/study/" + study.getPath() + "/settings/description";

        mockMvc.perform(post(url)
                .param("shortDescription", "short_short")
                .param("fullDescription", "full Description")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(url))
                .andExpect(flash().attributeExists("message"));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("스터디 소개 수정 - 실패")
    @Test
    public void updateDescription_fail() throws Exception {
        Account zering = accountRepository.findByNickname("zering");
        Study study = studyFactory.createStudy("test-study", zering);

        String url = "/study/" + study.getPath() + "/settings/description";

        mockMvc.perform(post(url)
                .param("shortDescription", "")
                .param("fullDescription", "full Description")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("studyDescriptionForm"));
    }
}